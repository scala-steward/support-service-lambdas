package com.gu.holidaystopprocessor

import java.time.LocalDate

import com.gu.holiday_stops._
import com.gu.salesforce.holiday_stops.SalesforceHolidayStopRequestsDetail.{ProductRatePlanKey, ProductRatePlanName, ProductType}
import com.softwaremill.sttp.{Id, SttpBackend}

object Products {
  val SundayVoucher = ProductRatePlanKey(ProductType("Newspaper Voucher"), ProductRatePlanName("Sunday"))
  val GuardianWeekly = ProductRatePlanKey(ProductType("Guardian Weekly"), ProductRatePlanName(""))
}

object HolidayStopProcess {
  import Products._
  def apply(config: Config, processDateOverride: Option[LocalDate], backend: SttpBackend[Id, Nothing]): List[ProcessResult] =
    Zuora.accessTokenGetResponse(config.zuoraConfig, backend) match {
      case Left(overallFailure) =>
        List(ProcessResult(overallFailure))

      case Right(zuoraAccessToken) =>

        List(
          CommonHolidayStopProcessor.processHolidayStops(
            config,
            Salesforce.holidayStopRequests(config.sfConfig)(SundayVoucher, processDateOverride),
            _, _, _
          ),
          CommonHolidayStopProcessor.processHolidayStops(
            config,
            Salesforce.holidayStopRequests(config.sfConfig)(GuardianWeekly, processDateOverride),
            _, _, _
          )
        ) map {
            _.apply(
              Zuora.subscriptionGetResponse(config, zuoraAccessToken, backend),
              Zuora.subscriptionUpdateResponse(config, zuoraAccessToken, backend),
              Salesforce.holidayStopUpdateResponse(config.sfConfig)
            )
          }
    }
}
