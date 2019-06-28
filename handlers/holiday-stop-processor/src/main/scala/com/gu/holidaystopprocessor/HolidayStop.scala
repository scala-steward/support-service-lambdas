package com.gu.holidaystopprocessor

import java.time.LocalDate

import com.gu.holiday_stops.ActionCalculator
import com.gu.salesforce.holiday_stops.SalesforceHolidayStopRequest.{HolidayStopRequest, HolidayStopRequestId}
import com.gu.util.Time

case class HolidayStop(
  requestId: HolidayStopRequestId,
  subscriptionName: String,
  stoppedPublicationDate: LocalDate
)

object HolidayStop {

  def holidayStopsToApply(getRequests: String => Either[OverallFailure, Seq[HolidayStopRequest]]): Either[OverallFailure, Seq[HolidayStop]] =
    getRequests("Guardian Weekly") map {
      _ flatMap toHolidayStops
    }

  private def toHolidayStops(request: HolidayStopRequest): Seq[HolidayStop] =
    ActionCalculator.publicationDatesToBeStopped(request) map { date =>
      HolidayStop(
        requestId = request.Id,
        subscriptionName = request.Subscription_Name__c.value,
        stoppedPublicationDate = Time.toJavaDate(date)
      )
    }
}