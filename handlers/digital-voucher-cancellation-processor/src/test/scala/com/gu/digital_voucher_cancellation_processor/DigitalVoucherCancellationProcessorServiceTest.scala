package com.gu.digital_voucher_cancellation_processor

import java.time.{Clock, Instant, LocalDate, ZoneId}

import cats.effect.IO
import com.gu.DevIdentity
import com.gu.digital_voucher_cancellation_processor.DigitalVoucherCancellationProcessorService.{DigitalVoucherQueryResult, DigitalVoucherUpdate, ImovoCancellationResults, SubscriptionQueryResult, subscrptionsCancelledTodayQuery}
import com.gu.imovo.{ImovoClientException, ImovoConfig, ImovoErrorResponse, ImovoSuccessResponse}
import com.gu.salesforce.sttp.{QueryRecordsWrapperCaseClass, SFApiCompositePart, SFApiCompositeRequest, SFApiCompositeResponse, SFApiCompositeResponsePart}
import com.gu.salesforce.{SFAuthConfig, SalesforceAuth}
import com.gu.imovo.ImovoStub._
import com.gu.salesforce.sttp.SalesforceStub._
import com.softwaremill.sttp.impl.cats.CatsMonadError
import com.softwaremill.sttp.testing.SttpBackendStub
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.gu.salesforce.sttp.SalesforceStub._
import io.circe.generic.auto._
import org.scalatest.Inside.inside

class DigitalVoucherCancellationProcessorServiceTest extends AnyFlatSpec with Matchers {
  val authConfig = SFAuthConfig(
    url = "https://unit-test.salesforce.com",
    client_id = "unit-test-client-id",
    client_secret = "unit-test-client-secret",
    username = "unit-tests@guardian.co.uk.dev",
    password = "unit-test-password",
    token = "unit-test-token"
  )

  val authResponse = SalesforceAuth(
    access_token = "unit-test-access-token",
    instance_url = "https://unit-test-instance-url.salesforce.com"
  )

  val imovoConfig = ImovoConfig("https://unit-test.imovo.com", "unit-test-imovo-api-key")

  val now = Instant.parse("2020-03-18T00:00:30.00Z")

  val testClock = Clock.fixed(now, ZoneId.systemDefault())

  def voucherToCancelQueryResult(resultId: String) = DigitalVoucherQueryResult(
    s"digital-voucher-id-$resultId",
    s"/services/data/v29.0/sobjects/Digital_Voucher__c/digital-voucher-id-$resultId",
    SubscriptionQueryResult(
      s"sf-subscription-id-$resultId",
      s"/services/data/v29.0/sobjects/SF_Subscription__c/sf-subscription-id-$resultId"
    )
  )

  private def salesforceVoucherUpdate(resultId: String) = {
    SFApiCompositePart(
      s"digital-voucher-id-$resultId",
      "PATCH",
      s"/services/data/v29.0/sobjects/Digital_Voucher__c/digital-voucher-id-$resultId",
      DigitalVoucherUpdate(now)
    )
  }

  "DigitalVoucherCancellationProcessor" should "query salesforce for subscriptions, call imovo to cancel sub " +
                                               "and update Cancellation_Process_On in SF" in {
    val salesforceBackendStub =
      SttpBackendStub[IO, Nothing](new CatsMonadError[IO])
        .stubAuth(authConfig, authResponse)
        .stubQuery(
          auth = authResponse,
          query = subscrptionsCancelledTodayQuery(LocalDate.parse("2020-03-18")),
          response = QueryRecordsWrapperCaseClass(
            List(
              voucherToCancelQueryResult("valid-sub-1"),
              voucherToCancelQueryResult("valid-sub-2")
            ),
            None
          )
        )
        .stubSubscriptionCancel(
          config = imovoConfig,
          subscriptionId = "sf-subscription-id-valid-sub-1",
          lastActiveDate = None,
          response = ImovoSuccessResponse("OK", true)
        )
        .stubSubscriptionCancel(
          config = imovoConfig,
          subscriptionId = "sf-subscription-id-valid-sub-2",
          lastActiveDate = None,
          response = ImovoSuccessResponse("OK", true)
        )
        .stubComposite(
          auth = authResponse,
          expectedRequest = Some(SFApiCompositeRequest(
            true,
            true,
            List(
              salesforceVoucherUpdate("valid-sub-1"),
              salesforceVoucherUpdate("valid-sub-2")
            )
          )),
          response = SFApiCompositeResponse(
            List(
              SFApiCompositeResponsePart(200, "VoucherUpdated"),
              SFApiCompositeResponsePart(200, "VoucherUpdated")
            )
          )
        )

    runApp(salesforceBackendStub, testClock) should ===(
      Right(
        ImovoCancellationResults(successfullyCancelled = List(
          voucherToCancelQueryResult("valid-sub-1"),
          voucherToCancelQueryResult("valid-sub-2"))
        )
      )
    )
  }
  it should "still update salesforce if subscription has already been cancelled in imovo" in {
    val salesforceBackendStub =
      SttpBackendStub[IO, Nothing](new CatsMonadError[IO])
        .stubAuth(authConfig, authResponse)
        .stubQuery(
          auth = authResponse,
          query = subscrptionsCancelledTodayQuery(LocalDate.parse("2020-03-18")),
          response = QueryRecordsWrapperCaseClass(
            List(
              voucherToCancelQueryResult("valid-sub"),
              voucherToCancelQueryResult("already-cancelled")
            ),
            None
          )
        )
        .stubSubscriptionCancel(
          config = imovoConfig,
          subscriptionId = "sf-subscription-id-valid-sub",
          lastActiveDate = None,
          response = ImovoSuccessResponse("OK", true)
        )
        .stubSubscriptionCancel(
          config = imovoConfig,
          subscriptionId = "sf-subscription-id-already-cancelled",
          lastActiveDate = None,
          response = ImovoErrorResponse(
            List("Unable to cancel vouchers: no live subscription vouchers exist for the supplied subscription id"),
            false
          )
        )
        .stubComposite(
          auth = authResponse,
          expectedRequest = Some(SFApiCompositeRequest(
            true,
            true,
            List(
              salesforceVoucherUpdate("valid-sub"),
              salesforceVoucherUpdate("already-cancelled")
            )
          )),
          response = SFApiCompositeResponse(
            List(
              SFApiCompositeResponsePart(200, "VoucherUpdated"),
              SFApiCompositeResponsePart(200, "VoucherUpdated")
            )
          )
        )

    runApp(salesforceBackendStub, testClock) should ===(
      Right(
        ImovoCancellationResults(
          successfullyCancelled = List(voucherToCancelQueryResult("valid-sub")),
          alreadyCancelled = List(voucherToCancelQueryResult("already-cancelled"))
        )
      )
    )
  }
  it should "not update salesforce if imovo request fails" in {
    val salesforceBackendStub =
      SttpBackendStub[IO, Nothing](new CatsMonadError[IO])
        .stubAuth(authConfig, authResponse)
        .stubQuery(
          auth = authResponse,
          query = subscrptionsCancelledTodayQuery(LocalDate.parse("2020-03-18")),
          response = QueryRecordsWrapperCaseClass(
            List(
              voucherToCancelQueryResult("valid-sub"),
              voucherToCancelQueryResult("imovo-failure")
            ),
            None
          )
        )
        .stubSubscriptionCancel(
          config = imovoConfig,
          subscriptionId = "sf-subscription-id-valid-sub",
          lastActiveDate = None,
          response = ImovoSuccessResponse("OK", true)
        )
        .stubSubscriptionCancel(
          config = imovoConfig,
          subscriptionId = "sf-subscription-id-imovo-failure",
          lastActiveDate = None,
          response = ImovoErrorResponse(
            List("Unexpected error"),
            false
          )
        )
        .stubComposite(
          auth = authResponse,
          expectedRequest = Some(SFApiCompositeRequest(
            true,
            true,
            List(
              salesforceVoucherUpdate("valid-sub")
            )
          )),
          response = SFApiCompositeResponse(
            List(
              SFApiCompositeResponsePart(200, "VoucherUpdated")
            )
          )
        )

    runApp(salesforceBackendStub, testClock) should ===(
      Right(
        ImovoCancellationResults(
          successfullyCancelled = List(voucherToCancelQueryResult("valid-sub")),
          cancellationFailures = List(
            ImovoClientException(
              """Request GET https://unit-test.imovo.com/Subscription/CancelSubscriptionVoucher?SubscriptionId=sf-subscription-id-imovo-failure failed with response ({
                |  "errorMessages" : [
                |    "Unexpected error"
                |  ],
                |  "successfulRequest" : false
                |})""".stripMargin
            )
          )
        )
      )
    )
  }
  it should "successfully return empty results if there are no subscriptions to cancel" in {
    val salesforceBackendStub =
      SttpBackendStub[IO, Nothing](new CatsMonadError[IO])
        .stubAuth(authConfig, authResponse)
        .stubQuery(
          auth = authResponse,
          query = subscrptionsCancelledTodayQuery(LocalDate.parse("2020-03-18")),
          response = QueryRecordsWrapperCaseClass(
            records = List[SFApiCompositePart[DigitalVoucherUpdate]](),
            nextRecordsUrl = None
          )
        )

    runApp(salesforceBackendStub, testClock) should ===(Right(ImovoCancellationResults()))
  }
  it should "return error if a salesforce update fails" in {
    val salesforceBackendStub =
      SttpBackendStub[IO, Nothing](new CatsMonadError[IO])
        .stubAuth(authConfig, authResponse)
        .stubQuery(
          auth = authResponse,
          query = subscrptionsCancelledTodayQuery(LocalDate.parse("2020-03-18")),
          response = QueryRecordsWrapperCaseClass(
            List(
              voucherToCancelQueryResult("valid-sub"),
              voucherToCancelQueryResult("salesforce-failure")
            ),
            None
          )
        )
        .stubSubscriptionCancel(
          config = imovoConfig,
          subscriptionId = "sf-subscription-id-valid-sub",
          lastActiveDate = None,
          response = ImovoSuccessResponse("OK", true)
        )
        .stubSubscriptionCancel(
          config = imovoConfig,
          subscriptionId = "sf-subscription-id-salesforce-failure",
          lastActiveDate = None,
          response = ImovoSuccessResponse("OK", true)
        )
        .stubComposite(
          auth = authResponse,
          expectedRequest = Some(SFApiCompositeRequest(
            true,
            true,
            List(
              salesforceVoucherUpdate("valid-sub"),
              salesforceVoucherUpdate("salesforce-failure")
            )
          )),
          response = SFApiCompositeResponse(
            List(
              SFApiCompositeResponsePart(200, "VoucherUpdated"),
              SFApiCompositeResponsePart(500, "VoucherUpdateFailed")
            )
          )
        )

    inside(runApp(salesforceBackendStub, testClock)) {
      case Left(DigitalVoucherCancellationProcessorAppError(message)) =>
        message should include("Failed to write changes to salesforce")

    }
  }


  private def runApp(salesforceBackendStub: SttpBackendStub[IO, Nothing], testClock: Clock) =
    DigitalVoucherCancellationProcessorApp(DevIdentity("digital-voucher-cancellation-processor"), salesforceBackendStub, testClock).value.unsafeRunSync()
}
