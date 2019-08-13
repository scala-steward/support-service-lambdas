package com.gu.holidaystopbackfill

import com.gu.effects.RawEffects
import com.gu.salesforce.SalesforceAuthenticate.SFAuthConfig
import com.gu.salesforce.SalesforceClient
import com.gu.salesforce.holiday_stops.SalesforceHolidayStopRequest.{CreateHolidayStopRequest, HolidayStopRequest, HolidayStopRequestEndDate, HolidayStopRequestStartDate, NewHolidayStopRequest}
import com.gu.salesforce.holiday_stops.SalesforceHolidayStopRequestsDetail.{ActionedHolidayStopRequestsDetailToBackfill, ProductName}
import com.gu.salesforce.holiday_stops.{SalesforceHolidayStopRequest, SalesforceHolidayStopRequestsDetail}
import com.gu.util.resthttp.JsonHttp
import scalaz.{-\/, \/-}

object Salesforce {

  def holidayStopRequestsByDateRangeAndProduct(sfCredentials: SFAuthConfig)(startDate: HolidayStopRequestStartDate, endDate: HolidayStopRequestEndDate, productNamePrefix: ProductName): Either[SalesforceFetchFailure, List[HolidayStopRequest]] =
    SalesforceClient(RawEffects.response, sfCredentials).value.flatMap { sfAuth =>
      val sfGet = sfAuth.wrapWith(JsonHttp.getWithParams)
      val fetchOp = SalesforceHolidayStopRequest.LookupByDateRangeAndProductNamePrefix(sfGet)
      fetchOp(startDate, endDate, productNamePrefix)
    }.toDisjunction match {
      case -\/(failure) => Left(SalesforceFetchFailure(failure.toString))
      case \/-(requests) => Right(requests)
    }

  def holidayStopDetailsCreateResponse(sfCredentials: SFAuthConfig)(details: List[ActionedHolidayStopRequestsDetailToBackfill]): Either[SalesforceUpdateFailure, Unit] =
    SalesforceClient(RawEffects.response, sfCredentials).value.map { sfAuth =>
      val sfPost = sfAuth.wrapWith(JsonHttp.post)
      val sendOp = SalesforceHolidayStopRequestsDetail.BackfillActionedSalesforceHolidayStopRequestsDetail(sfPost)
      details.map(sendOp).find(_.isFailure)
    }.toDisjunction match {
      case -\/(failure) => Left(SalesforceUpdateFailure(failure.toString))
      case _ => Right(())
    }

  def holidayStopCreateResponse(sfCredentials: SFAuthConfig)(requests: List[NewHolidayStopRequest]): Either[SalesforceUpdateFailure, Unit] =
    SalesforceClient(RawEffects.response, sfCredentials).value.map { sfAuth =>
      val sfGet = sfAuth.wrapWith(JsonHttp.post)
      val createOp = CreateHolidayStopRequest(sfGet)
      requests.map(createOp).find(_.isFailure)
    }.toDisjunction match {
      case -\/(failure) => Left(SalesforceUpdateFailure(failure.toString))
      case _ => Right(())
    }
}
