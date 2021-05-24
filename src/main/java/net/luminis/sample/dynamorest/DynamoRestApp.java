package net.luminis.sample.dynamorest;

import software.amazon.awscdk.core.App;

public class DynamoRestApp {
    public static void main(final String[] args) {
        App app = new App();

        new DynamoRestStack(app, "DynamoRestStack");

        app.synth();
    }
}
