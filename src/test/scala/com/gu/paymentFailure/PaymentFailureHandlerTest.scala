package com.gu.paymentFailure

import java.io.ByteArrayOutputStream

import com.gu.TestData._
import com.gu.TestingRawEffects
import com.gu.paymentFailure.PaymentFailureSteps.PFDeps
import com.gu.paymentFailure.ZuoraEmailSteps.ZuoraEmailStepsDeps
import com.gu.util.ETConfig.ETSendId
import com.gu.util.apigateway.{ ApiGatewayHandler, ApiGatewayRequest, ApiGatewayResponse }
import com.gu.util.exacttarget.EmailSendSteps.EmailSendStepsDeps
import com.gu.util.exacttarget._
import com.gu.util.reader.Types._
import com.gu.util.zuora.ZuoraModels._
import com.gu.util.{ Config, Stage }
import org.scalatest.{ FlatSpec, Matchers }

import scalaz.{ -\/, \/- }

class PaymentFailureHandlerTest extends FlatSpec with Matchers {

  "lambda" should "return error if credentials are missing" in {
    val stream = getClass.getResourceAsStream("/paymentFailure/missingCredentials.json")
    val os = new ByteArrayOutputStream()
    ApiGatewayHandler(lambdaConfig(basicOp()))(stream, os, null)
    val responseString = new String(os.toByteArray(), "UTF-8");
    responseString jsonMatches missingCredentialsResponse
  }

  "lambda" should "return error if credentials don't match" in {
    val stream = getClass.getResourceAsStream("/paymentFailure/invalidCredentials.json")
    val os = new ByteArrayOutputStream()
    ApiGatewayHandler(lambdaConfig(basicOp()))(stream, os, null)
    val responseString = new String(os.toByteArray(), "UTF-8");
    responseString jsonMatches missingCredentialsResponse
  }

  "lambda" should "return an error if tenant id doesn't match" in {
    val stream = getClass.getResourceAsStream("/paymentFailure/invalidTenant.json")
    val os = new ByteArrayOutputStream()
    ApiGatewayHandler(lambdaConfig(basicOp()))(stream, os, null)
    val responseString = new String(os.toByteArray(), "UTF-8");
    responseString jsonMatches missingCredentialsResponse
  }

  "lambda" should "enqueue email and return success for a valid request" in {
    //set up
    val stream = getClass.getResourceAsStream("/paymentFailure/validRequest.json")
    val output = new ByteArrayOutputStream
    var storedReq: Option[EmailRequest] = None

    val os = new ByteArrayOutputStream()
    //execute
    def configToFunction(config: Config): ApiGatewayRequest => FailableOp[Unit] = {
      PaymentFailureSteps.apply(PFDeps(
        ZuoraEmailSteps.sendEmailRegardingAccount(
          ZuoraEmailStepsDeps(
            EmailSendSteps.apply(
              EmailSendStepsDeps(
                req => {
                  storedReq = Some(req)
                  \/-(()): FailableOp[Unit]
                }, FilterEmail(Stage("PROD"))
              )
            ),
            a => \/-(basicInvoiceTransactionSummary)
          )
        ),
        config.etConfig.etSendIDs,
        config.trustedApiConfig
      ))
    }
    ApiGatewayHandler(lambdaConfig(configToFunction))(stream, os, null)

    //verify
    val responseString = new String(os.toByteArray, "UTF-8")

    val expectedMessage = EmailRequest(
      etSendId = ETSendId("11"),
      Message(
        To = ToDef(
          Address = "test.user123@guardian.co.uk",
          SubscriberKey = "test.user123@guardian.co.uk",
          ContactAttributes = ContactAttributesDef(
            SubscriberAttributes = SubscriberAttributesDef(
              subscriber_id = "A-S123",
              product = "Supporter",
              payment_method = "CreditCard",
              card_type = "Visa",
              card_expiry_date = "12/2017",
              first_name = "Test",
              last_name = "User",
              primaryKey = PaymentId("somePaymentId"),
              price = "£49.00",
              serviceStartDate = "21 November 2016",
              serviceEndDate = "21 December 2016"
            )
          )
        )
      )
    )

    storedReq should be(Some(expectedMessage))
    responseString jsonMatches successfulResponse
  }

  "lambda" should "return error if no additional data is found in zuora" in {
    //set up
    val stream = getClass.getResourceAsStream("/paymentFailure/validRequest.json")
    val output = new ByteArrayOutputStream
    val invoiceTransactionSummary = InvoiceTransactionSummary(List())

    val os = new ByteArrayOutputStream()

    //execute
    ApiGatewayHandler(lambdaConfig(basicOp(invoiceTransactionSummary)))(stream, os, null)

    //verify
    val responseString = new String(os.toByteArray(), "UTF-8")

    val expectedResponse = s"""{"statusCode":"500","headers":{"Content-Type":"application/json"},"body":"Failed to process event due to the following error: Could not retrieve additional data for account $accountId"} """
    responseString jsonMatches expectedResponse
  }

  def lambdaConfig(operation: Config => ApiGatewayRequest => FailableOp[Unit]) = new TestingRawEffects().handlerDeps(operation)
  def basicOp(fakeInvoiceTransactionSummary: InvoiceTransactionSummary = basicInvoiceTransactionSummary) = { config: Config =>
    PaymentFailureSteps.apply(PFDeps(
      ZuoraEmailSteps.sendEmailRegardingAccount(ZuoraEmailStepsDeps(
        sendEmail = _ => -\/(ApiGatewayResponse.internalServerError("something failed!")),
        getInvoiceTransactions = _ => \/-(fakeInvoiceTransactionSummary)
      )),
      config.etConfig.etSendIDs,
      config.trustedApiConfig
    )) _
  }

  "lambda" should "return error if message can't be queued" in {
    //set up
    val stream = getClass.getResourceAsStream("/paymentFailure/validRequest.json")
    val output = new ByteArrayOutputStream

    var storedReq: Option[EmailRequest] = None
    val os = new ByteArrayOutputStream()

    //execute
    def configToFunction(config: Config): ApiGatewayRequest => FailableOp[Unit] = {
      PaymentFailureSteps.apply(
        PFDeps(
          ZuoraEmailSteps.sendEmailRegardingAccount(
            ZuoraEmailStepsDeps(
              EmailSendSteps.apply(
                EmailSendStepsDeps(
                  req => {
                    storedReq = Some(req)
                    -\/(ApiGatewayResponse.internalServerError("something failed!")): FailableOp[Unit]
                  }, FilterEmail(Stage("PROD"))
                )
              ),
              a => \/-(basicInvoiceTransactionSummary)
            )
          ),
          config.etConfig.etSendIDs,
          config.trustedApiConfig
        )
      )
    }
    ApiGatewayHandler(lambdaConfig(configToFunction))(stream, os, null)

    //verify

    storedReq.isDefined should be(true)

    val responseString = new String(os.toByteArray(), "UTF-8")

    val expectedResponse = s"""{"statusCode":"500","headers":{"Content-Type":"application/json"},"body":"email not sent for account $accountId"} """
    responseString jsonMatches expectedResponse
  }

}