package com.gu.sf_billing_account_remover

import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import scalaj.http._

object BillingAccountRemover extends App {
  case class SfAuthDetails(access_token: String, instance_url: String)

  //Salesforce
  case class SfGetBillingAccsResponse(
    done: Boolean,
    records: Seq[BillingAccountsRecords.Records],
    nextRecordsUrl: Option[String] = None
  )
  case class SfGetCustomSettingResponse(
    done: Boolean,
    records: Seq[CustomSettingRecords.Records]
  )

  case class Attributes(`type`: String)

  case class SfBillingAccountToUpdate(id: String,
                                      GDPR_Removal_Attempts__c: Int,
                                      attributes: Attributes = Attributes(
                                        `type` = "Zuora__CustomerAccount__c"
                                      ))
  case class SfBillingAccountsToUpdate(allOrNone: Boolean,
                                       records: Seq[SfBillingAccountToUpdate])

  case class SfErrorRecordToCreate(Type__c: String,
                                   Info__c: String,
                                   Message__c: String,
                                   attributes: Attributes = Attributes(
                                     `type` = "Apex_Error__c"
                                   ))
  case class SfErrorRecordsToCreate(allOrNone: Boolean,
                                    records: Seq[SfErrorRecordToCreate])

  //Zuora
  case class BillingAccountsForRemoval(CrmId: String = "",
                                       Status: String = "Canceled")
  case class Errors(Code: String, Message: String)
  case class ZuoraResponse(Success: Boolean, Errors: Seq[Errors])

  val optConfig = for {
    sfUserName <- Option(System.getenv("username"))
    sfClientId <- Option(System.getenv("client_id"))
    sfClientSecret <- Option(System.getenv("client_secret"))
    sfPassword <- Option(System.getenv("password"))
    sfToken <- Option(System.getenv("token"))
    sfAuthUrl <- Option(System.getenv("authUrl"))

    zuoraApiAccessKeyId <- Option(System.getenv("apiAccessKeyId"))
    zuoraApiSecretAccessKey <- Option(System.getenv("apiSecretAccessKey"))

  } yield
    Config(
      SalesforceConfig(
        userName = sfUserName,
        clientId = sfClientId,
        clientSecret = sfClientSecret,
        password = sfPassword,
        token = sfToken,
        authUrl = sfAuthUrl
      ),
      ZuoraConfig(
        apiAccessKeyId = zuoraApiAccessKeyId,
        apiSecretAccessKey = zuoraApiSecretAccessKey
      )
    )

  var counter: Int = 0;
  processPage()

  def processPage(): Unit = {
    val t1 = System.nanoTime

    val iteration = for {
      config <- optConfig map (c => Right(c)) getOrElse Left(
        new RuntimeException("Missing config value")
      )
      token <- authToken(config.salesforceConfig)
      getCustomSettingResponse <- getSfCustomSetting(token)
      maxAttempts = getCustomSettingResponse.records(0).Property_Value__c
      getBillingAccountsResponse <- getSfBillingAccounts(maxAttempts, token)
      sfRecords = getBillingAccountsResponse.records

      failedUpdates = updateRecordsInZuora(sfRecords)
      _ <- updateBillingAccountsInSf(token, failedUpdates)
      _ <- insertErrorRecordsInSf(token, failedUpdates)
    } yield maxAttempts

    val duration = (System.nanoTime - t1) / 1e9d
    println("Elapsed time: " + duration + "s")
    println("maxAttempts: " + iteration)

  }

  def getSfBillingAccounts(
    maxAttempts: Int,
    bearerToken: String
  ): Either[Error, SfGetBillingAccsResponse] = {

    val billingAccountListFromSf =
      getBillingAccountsFromSf(maxAttempts: Int, bearerToken)
    println("billingAccountListFromSf: " + billingAccountListFromSf)
    val decodedBillingAccounts = decode[SfGetBillingAccsResponse](
      billingAccountListFromSf
    ) map { billingAccountsObject =>
      billingAccountsObject
    }

    decodedBillingAccounts
  }
  def getSfCustomSetting(
    bearerToken: String
  ): Either[Error, SfGetCustomSettingResponse] = {

    val customSettingFromSf =
      getMaxAttemptsCustomSetting(bearerToken)
    println("customSettingFromSf: " + customSettingFromSf)
    val decodedCustomSetting = decode[SfGetCustomSettingResponse](
      customSettingFromSf
    ) map { customSettingObject =>
      customSettingObject
    }

    decodedCustomSetting
  }

  def updateRecordsInZuora(
    recordsForUpdate: Seq[BillingAccountsRecords.Records]
  ): Seq[BillingAccountsRecords.Records] = {

    for {
      billingAccount <- recordsForUpdate
      zuoraCancellationResponses <- updateBillingAccountInZuora(billingAccount)
    } yield zuoraCancellationResponses

  }

  def updateBillingAccountsInSf(
    bearerToken: String,
    recordsToUpdate: Seq[BillingAccountsRecords.Records]
  ): Either[Throwable, String] = {

    val sfBillingAccUpdateJson = SfUpdateBillingAccounts(recordsToUpdate).asJson.spaces2
    updateSfBillingAccs(bearerToken, sfBillingAccUpdateJson)

  }

  def insertErrorRecordsInSf(
    bearerToken: String,
    recordsToUpdate: Seq[BillingAccountsRecords.Records]
  ): Either[Throwable, String] = {

    val sfErrorRecordInsertJson = SfCreateErrorRecords(recordsToUpdate).asJson.spaces2
    insertSfErrorRecs(bearerToken, sfErrorRecordInsertJson)

  }

  def updateBillingAccountInZuora(
    accountToDelete: BillingAccountsRecords.Records
  ): Option[BillingAccountsRecords.Records] = {
    counter = counter + 1
    val response =
      updateZuoraBillingAcc(
        BillingAccountsForRemoval().asJson.spaces2,
        accountToDelete.Zuora__External_Id__c
      )
    println(counter + " | response:" + response)
    val parsedResponse = decode[ZuoraResponse](response)

    parsedResponse match {
      case Right(dr) =>
        if (dr.Success) {
          None
        } else {
          Some(
            accountToDelete.copy(
              ErrorCode = Some(dr.Errors(0).Code),
              ErrorMessage = Some(dr.Errors(0).Message)
            )
          )
        }
      case Left(ex) => {
        Some(accountToDelete.copy(ErrorMessage = Some(ex.toString)))
      }
    }

  }

  def insertSfErrorRecs(bearerToken: String,
                        jsonBody: String): Either[Throwable, String] = {

    val response =
      try {
        Right(
          Http(
            "https://gnmtouchpoint--dev.my.salesforce.com/services/data/v45.0/composite/sobjects"
          ).header("Authorization", s"Bearer $bearerToken")
            .header("Content-Type", "application/json")
            .put(jsonBody)
            .method("POST")
            .asString
            .body
        )
      } catch {
        case ex: Throwable => Left(ex)
      }
    response
  }

  def updateSfBillingAccs(bearerToken: String,
                          jsonBody: String): Either[Throwable, String] = {

    val response =
      try {
        Right(
          Http(
            "https://gnmtouchpoint--dev.my.salesforce.com/services/data/v45.0/composite/sobjects"
          ).header("Authorization", s"Bearer $bearerToken")
            .header("Content-Type", "application/json")
            .put(jsonBody)
            .method("PATCH")
            .asString
            .body
        )
      } catch {
        case ex: Throwable => Left(ex)
      }
    response
  }

  def updateZuoraBillingAcc(billingAccountForRemovalAsJson: String,
                            zuoraBillingAccountId: String) = {
    Http(
      s"https://rest.apisandbox.zuora.com/v1/object/account/$zuoraBillingAccountId"
    ).header("apiAccessKeyId", System.getenv("apiAccessKeyId"))
      .header("apiSecretAccessKey", System.getenv("apiSecretAccessKey"))
      .header("Content-Type", "application/json")
      .put(billingAccountForRemovalAsJson)
      .asString
      .body
  }

  def parseBillingAccountsFromSf(
    billingAccountList: String
  ): Either[Exception, Seq[BillingAccountsRecords.Records]] =
    decode[SfGetBillingAccsResponse](billingAccountList) map {
      billingAccountsObject =>
        billingAccountsObject.records
    }

  def getBillingAccountsFromSf(maxAttempts: Int,
                               bearerToken: String): String = {
    val limit = 1;
    val query =
      s"Select Id, Zuora__Account__c, GDPR_Removal_Attempts__c, Zuora__External_Id__c from Zuora__CustomerAccount__c where Active_Subs__c > 0 and Zuora__External_Id__c != null and GDPR_Removal_Attempts__c < $maxAttempts limit $limit"

    println("query:" + query)
    Http(
      s"https://gnmtouchpoint--dev.my.salesforce.com/services/data/v20.0/query/"
    ).param("q", query)
      .option(HttpOptions.readTimeout(30000))
      .header("Authorization", s"Bearer $bearerToken")
      .method("GET")
      .asString
      .body
  }

  def getMaxAttemptsCustomSetting(bearerToken: String): String = {

    val query: String =
      "Select Id, Property_Value__c from Touch_Point_List_Property__c where name = 'Max Billing Acc GDPR Removal Attempts'"
    Http(
      s"https://gnmtouchpoint--dev.my.salesforce.com/services/data/v20.0/query/"
    ).param("q", query)
      .option(HttpOptions.readTimeout(30000))
      .header("Authorization", s"Bearer $bearerToken")
      .method("GET")
      .asString
      .body
  }

  def auth(salesforceConfig: SalesforceConfig): String = {
    Http(s"${System.getenv("authUrl")}/services/oauth2/token")
      .postForm(
        Seq(
          "grant_type" -> "password",
          "client_id" -> salesforceConfig.clientId,
          "client_secret" -> salesforceConfig.clientSecret,
          "username" -> salesforceConfig.userName,
          "password" -> s"${salesforceConfig.password}${salesforceConfig.token}"
        )
      )
      .asString
      .body

  }

  def authToken(salesforceConfig: SalesforceConfig) =
    decode[SfAuthDetails](auth(salesforceConfig)) match {
      case Right(responseObject) => Right(responseObject.access_token)
      case Left(ex)              => Left(ex)
    }

  object SfUpdateBillingAccounts {
    def apply(
      recordList: Seq[BillingAccountsRecords.Records]
    ): SfBillingAccountsToUpdate = {

      val recordListWithincrementedGDPRAttempts =
        recordList.map(
          a => a.copy(GDPR_Removal_Attempts__c = a.GDPR_Removal_Attempts__c + 1)
        )

      val sfBillingAccounts = recordListWithincrementedGDPRAttempts
        .map(a => SfBillingAccountToUpdate(a.Id, a.GDPR_Removal_Attempts__c))
        .toSeq

      SfBillingAccountsToUpdate(false, sfBillingAccounts)
    }
  }

  object SfCreateErrorRecords {
    def apply(
      recordList: Seq[BillingAccountsRecords.Records]
    ): SfErrorRecordsToCreate = {

      val sfErrorRecords = recordList
        .map(
          a =>
            SfErrorRecordToCreate(
              Type__c = a.ErrorCode.get,
              Info__c = "Billing Account Id:" + a.Id,
              Message__c = a.ErrorMessage.get
          )
        )
        .toSeq

      SfErrorRecordsToCreate(false, sfErrorRecords)
    }
  }
}
