package com.gu.identityBackfill.zuora

import com.gu.identityBackfill.Types._
import com.gu.identityBackfill.zuora.GetZuoraAccountsForEmail.WireModel._
import com.gu.util.apigateway.ApiGatewayResponse
import com.gu.util.reader.Types.FailableOp
import com.gu.util.zuora.ZuoraQuery.Query
import com.gu.util.zuora.{ZuoraDeps, ZuoraQuery}
import play.api.libs.json.Json

import scalaz.ListT

object GetZuoraAccountsForEmail {

  object WireModel {

    case class ZuoraContact(Id: String)
    implicit val zcReads = Json.reads[ZuoraContact]
    case class ZuoraAccount(
      Id: String,
      IdentityId__c: Option[String],
      sfContactId__c: String
    )
    implicit val zaReads = Json.reads[ZuoraAccount]

  }

  def apply(zuoraDeps: ZuoraDeps)(emailAddress: EmailAddress): FailableOp[List[ZuoraAccountIdentitySFContact]] = {
    val accounts = for {
      contactWithEmail <- {
        val contactQuery = Query(s"SELECT Id FROM Contact where WorkEmail='${emailAddress.value}'")
        ListT(ZuoraQuery.getResults[ZuoraContact](contactQuery).map(_.records))
      }
      accountsWithEmail <- {
        val accountQuery = Query(s"SELECT Id, IdentityId__c, sfContactId__c FROM Account where BillToId='${contactWithEmail.Id}'")
        ListT(ZuoraQuery.getResults[ZuoraAccount](accountQuery).map(_.records))
      }
    } yield ZuoraAccountIdentitySFContact(
      AccountId(accountsWithEmail.Id),
      accountsWithEmail.IdentityId__c.map(IdentityId.apply),
      SFContactId(accountsWithEmail.sfContactId__c)
    )

    accounts.run.run.run(zuoraDeps).leftMap(e => ApiGatewayResponse.internalServerError(e.message))
  }

}