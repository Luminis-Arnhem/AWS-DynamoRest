package net.luminis.sample.dynamorest;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.iam.*;

import java.util.List;
import java.util.Map;


public class DynamoRestStack extends Stack {
    public DynamoRestStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public DynamoRestStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        RestApi dynamoRestApi = new RestApi(this, "DynamoRest", RestApiProps.builder()
                .deployOptions(StageOptions.builder()
                        .loggingLevel(MethodLoggingLevel.INFO)
                        .build())
                .build());


        TableProps tableProps = TableProps.builder()
                .partitionKey(Attribute.builder()
                        .name("id")
                        .type(AttributeType.STRING)
                        .build())
                .tableName("exerciseStats")
                .build();
        Table dynamoDbTable = new Table(this, "exerciseStats", tableProps);
        String tableName = dynamoDbTable.getTableName();


        Role role = new Role(this, "api-gateway-accesses-dynamodb-role", RoleProps.builder()
                .assumedBy(new ServicePrincipal("apigateway.amazonaws.com"))
                .build());
        role.addToPolicy(new PolicyStatement(PolicyStatementProps.builder()
                .actions(List.of("dynamodb:Query", "dynamodb:PutItem", "dynamodb:UpdateItem"))
                .effect(Effect.ALLOW)
                .resources(List.of(dynamoDbTable.getTableArn()))
                .build()));


        // Define integration details for POST method (resource creation)
        String createRequestTemplate = "{\n" +
                "    \"TableName\": \"" + tableName + "\",\n" +
                "    \"Item\": {\n" +
                "\t    \"id\": {\n" +
                "            \"S\": \"$input.params('id')\"\n" +
                "        },\n" +
                "        \"content\": {\n" +
                "            \"S\": \"$util.escapeJavaScript($input.body)\"\n" +
                "        }\n" +
                "    }\n" +
                "}";

        Integration createIntegration = AwsIntegration.Builder.create()
                .action("PutItem")
                .service("dynamodb")
                .integrationHttpMethod("POST")
                .options(IntegrationOptions.builder()
                        .credentialsRole(role)
                        .requestTemplates(Map.of("application/json", createRequestTemplate))
                        .integrationResponses(List.of(IntegrationResponse.builder()
                                .statusCode("200")
                                .build()))
                        .build())
                .build();

        IResource restApiRootResource = dynamoRestApi.getRoot();
        restApiRootResource.addMethod("POST", createIntegration, MethodOptions.builder()
                        .methodResponses(List.of(MethodResponse.builder()
                                .statusCode("200")
                                .build()))
                        .build());


        // Define integration details for PUT method (resource update)
        String updateRequestTemplate = "{\n" +
                "    \"TableName\": \"" + tableName + "\",\n" +
                "    \"Key\": {\n" +
                "        \"id\": {\n" +
                "            \"S\": \"$input.params('id')\"\n" +
                "        }\n" +
                "    },\n" +
                "    \"UpdateExpression\": \"set content = :v1\",\n" +
                "    \"ExpressionAttributeValues\": {\n" +
                "        \":v1\": {\n" +
                "            \"S\": \"$util.escapeJavaScript($input.body)\"\n" +
                "        }\n" +
                "    },\n" +
                "    \"ReturnValues\": \"NONE\"\n" +
                "}";

        Integration updateIntegration = AwsIntegration.Builder.create()
                .action("UpdateItem")
                .service("dynamodb")
                .integrationHttpMethod("POST")
                .options(IntegrationOptions.builder()
                        .credentialsRole(role)
                        .requestTemplates(Map.of("application/json", updateRequestTemplate))
                        .integrationResponses(List.of(IntegrationResponse.builder()
                                .statusCode("200")
                                .build()))
                        .build())
                .build();

        Resource documentResource = dynamoRestApi.getRoot().addResource("{id}");
        documentResource.addMethod("PUT", updateIntegration, MethodOptions.builder()
                .methodResponses(List.of(MethodResponse.builder()
                        .statusCode("200")
                        .build()))
                .build());


        // Define integration details for GET method (resource retrieval)
        String getRequestTemplate = "{\n" +
                " \"TableName\": \"" + tableName + "\",\n" +
                " \"KeyConditionExpression\": \"id = :v1\",\n" +
                "    \"ExpressionAttributeValues\": {\n" +
                "        \":v1\": {\n" +
                "            \"S\": \"$input.params('id')\"\n" +
                "        }\n" +
                "    }\n" +
                "}";

        Integration queryIntegration = AwsIntegration.Builder.create()
                .action("Query")
                .service("dynamodb")
                .integrationHttpMethod("POST")
                .options(IntegrationOptions.builder()
                        .credentialsRole(role)
                        .requestTemplates(Map.of("application/json", getRequestTemplate))
                        .integrationResponses(List.of(IntegrationResponse.builder()
                                .statusCode("200")
                                .responseTemplates(Map.of("application/json", "#set($inputRoot = $input.path('$'))\n" +
                                                "#if(!$inputRoot.Items.isEmpty())$inputRoot.Items[0].content.S\n" +
                                                "#end\n"))
                                .build()))
                        .build())
                .build();

        documentResource.addMethod("GET", queryIntegration, MethodOptions.builder()
                        .methodResponses(List.of(MethodResponse.builder()
                                .statusCode("200")
                                .build()))
                        .build());
    }
}
