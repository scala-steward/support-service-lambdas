package com.gu.holiday_stops

import java.time.{LocalDate, ZonedDateTime}

import cats.implicits._
import com.gu.holiday_stops.subscription.Subscription
import com.gu.salesforce.holiday_stops.SalesforceHolidayStopRequest._
import com.gu.salesforce.holiday_stops.SalesforceHolidayStopRequestsDetail
import com.gu.salesforce.holiday_stops.SalesforceHolidayStopRequestsDetail.SubscriptionName
import play.api.libs.json.{Json, OFormat}

object WireHolidayStopRequest {

  def apply(issueSpecifics: List[IssueSpecifics])(sfHolidayStopRequest: HolidayStopRequest): HolidayStopRequestFull = HolidayStopRequestFull(
    id = sfHolidayStopRequest.Id.value,
    start = sfHolidayStopRequest.Start_Date__c.value,
    end = sfHolidayStopRequest.End_Date__c.value,
    subscriptionName = sfHolidayStopRequest.Subscription_Name__c,
    publicationsImpacted = sfHolidayStopRequest
      .Holiday_Stop_Request_Detail__r
      .map(_.records.map(toHolidayStopRequestDetail)).getOrElse(List()),
    withdrawnTime = sfHolidayStopRequest.Withdrawn_Time__c.map(_.value),
    mutabilityFlags = calculateMutabilityFlags(
      isWithdrawn = sfHolidayStopRequest.Is_Withdrawn__c.value,
      firstAvailableDate = issueSpecifics.map(_.firstAvailableDate).min[LocalDate](_ compareTo _),
      actionedCount = sfHolidayStopRequest.Actioned_Count__c.value,
      firstPublicationDate = sfHolidayStopRequest.Holiday_Stop_Request_Detail__r.map(_.records.map(_.Stopped_Publication_Date__c.value).min[LocalDate](_ compareTo _)).get,
      lastPublicationDate = sfHolidayStopRequest.Holiday_Stop_Request_Detail__r.map(_.records.map(_.Stopped_Publication_Date__c.value).max[LocalDate](_ compareTo _)).get
    )
  )

  def toHolidayStopRequestDetail(detail: SalesforceHolidayStopRequestsDetail.HolidayStopRequestsDetail) = {
    HolidayStopRequestsDetail(
      publicationDate = detail.Stopped_Publication_Date__c.value,
      estimatedPrice = detail.Estimated_Price__c.map(_.value),
      actualPrice = detail.Actual_Price__c.map(_.value),
      invoiceDate = detail.Expected_Invoice_Date__c.map(_.value)
    )
  }

  def calculateMutabilityFlags(
    isWithdrawn: Boolean,
    firstAvailableDate: LocalDate,
    actionedCount: Int,
    firstPublicationDate: LocalDate,
    lastPublicationDate: LocalDate
  ): MutabilityFlags = {
    if (actionedCount == 0 && firstAvailableDate.isAfter(firstPublicationDate.plusDays(2))) {
      // TODO log warning (with CloudWatch alert) as indicates processing of holiday stop is well overdue
    }
    val firstPublicationDateIsGreaterOrEqualToFirstAvailableDate =
      firstPublicationDate.isEqual(firstAvailableDate) || firstPublicationDate.isAfter(firstAvailableDate)
    MutabilityFlags(
      isFullyMutable = !isWithdrawn && actionedCount == 0 && firstPublicationDateIsGreaterOrEqualToFirstAvailableDate,
      isEndDateEditable = !isWithdrawn && firstAvailableDate.isBefore(lastPublicationDate)
    )
  }

}

case class MutabilityFlags(
  isFullyMutable: Boolean,
  isEndDateEditable: Boolean
)

object MutabilityFlags {
  implicit val format: OFormat[MutabilityFlags] = Json.format[MutabilityFlags]
}

case class HolidayStopRequestsDetail(
  publicationDate: LocalDate,
  estimatedPrice: Option[Double],
  actualPrice: Option[Double],
  invoiceDate: Option[LocalDate]
)

object HolidayStopRequestsDetail {
  implicit val format: OFormat[HolidayStopRequestsDetail] = Json.format[HolidayStopRequestsDetail]
}

case class HolidayStopRequestPartial(
  start: LocalDate,
  end: LocalDate,
  subscriptionName: SubscriptionName
)

object HolidayStopRequestPartial {
  implicit val format: OFormat[HolidayStopRequestPartial] = Json.format[HolidayStopRequestPartial]
}

case class HolidayStopRequestFull(
  id: String,
  start: LocalDate,
  end: LocalDate,
  subscriptionName: SubscriptionName,
  publicationsImpacted: List[HolidayStopRequestsDetail],
  withdrawnTime: Option[ZonedDateTime],
  mutabilityFlags: MutabilityFlags
)

object HolidayStopRequestFull {
  implicit val format: OFormat[HolidayStopRequestFull] = Json.format[HolidayStopRequestFull]
}

case class GetHolidayStopRequests(
  existing: List[HolidayStopRequestFull],
  issueSpecifics: List[IssueSpecifics],
  annualIssueLimit: Int
)

object GetHolidayStopRequests {

  def apply(
    holidayStopRequests: List[HolidayStopRequest],
    subscription: Subscription
  ): Either[GetHolidayStopRequestsError, GetHolidayStopRequests] =
    ActionCalculator
      .getProductSpecificsByProductVariant(ProductVariant(subscription.ratePlans), subscription)
      .leftMap(error => GetHolidayStopRequestsError(s"Failed to get product specifics for $subscription: $error"))
      .map(productSpecifics =>
        GetHolidayStopRequests(
          existing = holidayStopRequests.map(WireHolidayStopRequest.apply(productSpecifics.issueSpecifics)),
          issueSpecifics = productSpecifics.issueSpecifics,
          annualIssueLimit = productSpecifics.annualIssueLimit
        ))

  implicit val formatIssueSpecifics: OFormat[IssueSpecifics] = Json.format[IssueSpecifics]
  implicit val formatProductSpecifics: OFormat[ProductSpecifics] = Json.format[ProductSpecifics]
  implicit val format: OFormat[GetHolidayStopRequests] = Json.format[GetHolidayStopRequests]
}

case class GetHolidayStopRequestsError(message: String)

case class GetCancellationDetails(publicationsToRefund: List[HolidayStopRequestsDetail])
object GetCancellationDetails {
  implicit val formatGetCancellationDetails = Json.format[GetCancellationDetails]
}
