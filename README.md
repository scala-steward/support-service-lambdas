# zuora-auto-cancel

This Scala Lambda is used to cancel subscriptions and memberships with overdue invoices, based on an event trigger within Zuora.

The full workflow is currently:
Zuora Callout > AWS CloudFront > AWS API Gateway > AWS Lambda (zuora-auto-cancel-STAGE)

# Running locally

When the Lambda is triggered, AWS uses the handleRequest method as the entry point. However, uploading new Lambda code to test every change during development would be a laborious process.

All of the 'real work' is handled by cancellationAttemptForPayload, so to get fast feedback when developing simply call this method with a sample payload and execute `sbt run`.

Note: to make REST API calls to Zuora, you will need to set the following environment variables when running locally:
- ZuoraRestUrl - for testing purposes this is: https://rest.apisandbox.zuora.com/v1
- ZuoraUsername
- ZuoraPassword

(For Lambda execution, these are set as encrypted environment variables within AWS).

# Testing

- Run `sbt test` to execute the unit tests
- If you need to validate that API Gateway can trigger Lambda execution successfully, deploy your changes to CODE and use Postman (or equivalent) to hit the API Gateway url with a valid payload.
- For a full integration test, you can trigger the callout from our UAT Zuora environment.