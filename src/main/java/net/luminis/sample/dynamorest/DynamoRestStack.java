package net.luminis.sample.dynamorest;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.apigateway.AuthorizationType;
import software.amazon.awscdk.services.apigateway.MethodOptions;
import software.amazon.awscdk.services.apigateway.RestApiProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.amazon.awsconstructs.services.apigatewaydynamodb.ApiGatewayToDynamoDB;
import software.amazon.awsconstructs.services.apigatewaydynamodb.ApiGatewayToDynamoDBProps;

public class DynamoRestStack extends Stack {
    public DynamoRestStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public DynamoRestStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        RestApiProps apiGatewayProps = RestApiProps.builder()
                .restApiName("DynamoRest")
                .defaultMethodOptions(MethodOptions.builder()
                        .authorizationType(AuthorizationType.NONE)
                        .build())
                .build();

        TableProps tableProps = TableProps.builder()
                .partitionKey(Attribute.builder()
                        .name("id")
                        .type(AttributeType.STRING)
                        .build())
                .tableName("exerciseStats")
                .build();

        String tableName = tableProps.getTableName();
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

        ApiGatewayToDynamoDBProps apiGatewayToDynamoDBProps = ApiGatewayToDynamoDBProps.builder()
                .apiGatewayProps(apiGatewayProps)
                .dynamoTableProps(tableProps)
                .allowCreateOperation(true)
                .createRequestTemplate(createRequestTemplate)
                .allowUpdateOperation(true)
                .updateRequestTemplate(updateRequestTemplate)
                .allowReadOperation(true)
                .build();

        ApiGatewayToDynamoDB apiGateway = new ApiGatewayToDynamoDB(this, "dynamogateway", apiGatewayToDynamoDBProps);
    }
}
