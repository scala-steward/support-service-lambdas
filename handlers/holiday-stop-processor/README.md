# Holiday Stop Processor

This lambda updates subscriptions in Zuora with holiday stop amendments.  

On a regular schedule it:
1. reads holiday stop requests from Salesforce,
1. for each request, it applies a discount amendment to the appropriate subscription in Zuora effective on the first day of the next billing period for that subscription,
1. writes back to Salesforce details of the amendments made.

The requests fetched from Salesforce depend on the type of product that the request affects.  There's a set of rules for each publication.  It's a just-in-time process so that amendments are only applied at the last moment before the fulfilment of that publication can be altered.  Every publication has a lead time for its fulfilment during which it cannot be altered: typically this is a period of 10 to 14 days.

There is a separate holiday credit amendment applied in Zuora for each individual stopped publication date.

## Configuration
The lambda is deployed in [Code](https://eu-west-1.console.aws.amazon.com/lambda/home?region=eu-west-1#/functions/holiday-stop-processor-CODE) and [Prod](https://eu-west-1.console.aws.amazon.com/lambda/home?region=eu-west-1#/functions/holiday-stop-processor-PROD) environments in the Membership account through [Riff Raff](https://riffraff.gutools.co.uk/deployment/history?projectName=MemSub%3A%3AMembership%20Admin%3A%3Aholiday-stop-processor).
* The Cloud Formation script is [here](cfn.yaml).
* The Riff Raff configuration is [here](riff-raff.yaml).
* The Zuora and Salesforce credentials are stored in stage-specific folders in S3.

## Input
The lambda has no direct inputs.  Its configuration is fetched as part of the script.
 
## Output
The output is a list of successfully applied holiday credits corresponding to the requests fetched from Salesforce.  These may not have been applied as part of the current run of the lambda; they may have been applied in previous runs.  But this ensures there is a complete list of results corresponding to the list of holiday requests fetched.
 
## Effects

The lambda has two direct effects and a third indirect effect:  

#### Zuora
It applies an amendment to relevant subscriptions in Zuora, 

#### Salesforce
It writes holiday request details back to Salesforce, when those details have not already been written to Salesforce.  
* The Prod holiday stop requests table is [here](https://eu7.salesforce.com/a2k).
* The Prod holiday stop details table is [here](https://eu7.salesforce.com/a2j). 

#### S3
The indirect effect of the lambda is to add entries to the daily updated weekly stops CSV report in [S3](https://s3.console.aws.amazon.com/s3/buckets/fulfilment-export-prod/zuoraExport).
The files have the name format `WeeklyHolidaySuspensions_YYYY-MM-DD_UUID.csv`.  
This is generated by a [ZOQL query](https://github.com/guardian/fulfilment-lambdas/blob/master/src/weekly/query.js#L114-L129) over the applied holiday credit amendments.

## To test
As well as a suite of unit tests the codebase includes a [standalone app](src/main/scala/com/gu/holidaystopprocessor/StandaloneApp.scala) 
that's a thin wrapper around the processor to enable it to be tested functionally from a dev machine.

To test the lambda in the Code environment you can run the `assembly` task and then deploy the resulting artefact by means of the AWS UI.  Alternatively Riff Raff has been configured to deploy any branch that has successfully passed through TeamCity.  That will take longer than assembling locally but will be independent of the idiosyncracies of any particular dev environment.

## Backfilling Salesforce
See the [readme](src/main/scala/com/gu/README.md).