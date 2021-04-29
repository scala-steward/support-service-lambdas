package com.gu.soft_opt_in_consent_setter

import com.gu.soft_opt_in_consent_setter.models.{EnhancedCancelledSub, SFAssociatedSubResponse, SFSubRecord, SFSubRecordUpdate, SFSubRecordUpdateRequest, SoftOptInConfig, SoftOptInError}
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import io.circe.syntax._

// TODO: introduce notifications when number of attempts is incremented to 5

object Main extends App with LazyLogging {

  val readyToProcessAcquisitionStatus = "Ready to process acquisition"
  val readyToProcessCancellationStatus = "Ready to process cancellation"

  (for {
    config <- SoftOptInConfig()
    sfConnector <- SalesforceConnector(config.sfConfig, config.sfApiVersion)

    allSubs <- sfConnector.getSubsToProcess()
    identityConnector = new IdentityConnector(config.identityConfig)
    consentsCalculator = new ConsentsCalculator(config.consentsMapping)

    acqSubs = allSubs.records.filter(_.Soft_Opt_in_Status__c.equals(readyToProcessAcquisitionStatus))
    _ <- processAcquiredSubs(acqSubs, identityConnector.sendConsentsReq, sfConnector.updateSubs, consentsCalculator)

    cancelledSubs = allSubs.records.filter(_.Soft_Opt_in_Status__c.equals(readyToProcessCancellationStatus))
    cancelledSubsIdentityIds = cancelledSubs.map(sub => sub.Buyer__r.IdentityID__c)

    activeSubs <- sfConnector.getActiveSubs(cancelledSubsIdentityIds)
    _ <- processCancelledSubs(cancelledSubs, activeSubs, identityConnector.sendConsentsReq, sfConnector.updateSubs, consentsCalculator)
  } yield ())
    .left
    .map(error => {
      // TODO: Surface this error outside of the lambda for alarm purposes
      logger.error(s"${error.errorType}: ${error.errorDetails}")
    })

  def processAcquiredSubs(acquiredSubs: Seq[SFSubRecord], sendConsentsReq: (String, String) => Either[SoftOptInError, Unit], updateSubs: String => Either[SoftOptInError, Unit], consentsCalculator: ConsentsCalculator): Either[SoftOptInError, Unit] = {
    val recordsToUpdate = acquiredSubs
      .map(sub => {
        SFSubRecordUpdate(sub, "Acquisition",
          for {
            consents <- consentsCalculator.getAcquisitionConsents(sub.Product__c)
            consentsBody = consentsCalculator.buildConsentsBody(consents, state = true)
            _ <- sendConsentsReq(sub.Buyer__r.IdentityID__c, consentsBody)
          } yield ())
      })

    if (recordsToUpdate.isEmpty)
      Right(())
    else
      updateSubs(SFSubRecordUpdateRequest(recordsToUpdate).asJson.spaces2)
  }

  def processCancelledSubs(cancelledSubs: Seq[SFSubRecord], activeSubs: SFAssociatedSubResponse, sendConsentsReq: (String, String) => Either[SoftOptInError, Unit], updateSubs: String => Either[SoftOptInError, Unit], consentsCalculator: ConsentsCalculator): Either[SoftOptInError, Unit] = {
    def sendCancellationConsents(identityId: String, consents: Set[String]): Either[SoftOptInError, Unit] = {
      if (consents.nonEmpty)
        sendConsentsReq(
          identityId,
          consentsCalculator.buildConsentsBody(consents, state = false)
        )
      else
        Right(())
    }

    val recordsToUpdate = cancelledSubs
      .map(EnhancedCancelledSub(_, activeSubs.records))
      .map(sub => {
        SFSubRecordUpdate(sub.cancelledSub, "Cancellation",
          for {
            consents <- consentsCalculator.getCancellationConsents(sub.cancelledSub.Product__c, sub.associatedActiveNonGiftSubs.map(_.Product__c).toSet)
            _ <- sendCancellationConsents(sub.identityId, consents)
          } yield ())
      })

    if (recordsToUpdate.isEmpty)
      Right(())
    else
      updateSubs(SFSubRecordUpdateRequest(recordsToUpdate).asJson.spaces2)
  }

}