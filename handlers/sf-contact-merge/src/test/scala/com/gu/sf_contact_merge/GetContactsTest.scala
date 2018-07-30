package com.gu.sf_contact_merge

import com.gu.sf_contact_merge.validation.GetContacts
import com.gu.sf_contact_merge.validation.GetContacts.{Account, AccountId, IdentityId, SFContactId}
import com.gu.sf_contact_merge.validation.GetEmails.ContactId
import com.gu.util.resthttp.Types.ClientSuccess
import com.gu.zuora.fake.FakeZuoraQuerier
import org.scalatest.{FlatSpec, Matchers}
import scalaz.NonEmptyList

class GetContactsTest extends FlatSpec with Matchers {

  import GetContactsTest._

  it should "work" in {

    val zuoraQuerier = FakeZuoraQuerier(accountQueryRequest, accountQueryResponse)
    val getContacts = GetContacts(zuoraQuerier)_
    val actual = getContacts(NonEmptyList(
      AccountId("acid1"),
      AccountId("acid2")
    ))

    actual should be(ClientSuccess(Map(
      ContactId("b2id1") -> Account(Some(IdentityId("idid1")), SFContactId("sfsf1")),
      ContactId("b2id2") -> Account(Some(IdentityId("idid2")), SFContactId("sfsf2"))
    )))

  }

}

object GetContactsTest {

  val accountQueryRequest =
    """SELECT BillToId, IdentityId__c, sfContactId__c FROM Account WHERE Id = 'acid1' or Id = 'acid2'"""

  val accountQueryResponse =
    """{
      |    "records": [
      |        {
      |            "BillToId": "b2id1",
      |            "Id": "acid1",
      |            "IdentityId__c": "idid1",
      |            "sfContactId__c": "sfsf1"
      |        },
      |        {
      |            "BillToId": "b2id2",
      |            "Id": "acid2",
      |            "IdentityId__c": "idid2",
      |            "sfContactId__c": "sfsf2"
      |        }
      |    ],
      |    "size": 2,
      |    "done": true
      |}""".stripMargin

}
