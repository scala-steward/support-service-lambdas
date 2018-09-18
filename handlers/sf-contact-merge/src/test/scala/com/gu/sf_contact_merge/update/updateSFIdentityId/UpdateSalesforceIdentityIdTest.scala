package com.gu.sf_contact_merge.update.updateSFIdentityId

import com.gu.salesforce.TypesForSFEffectsData.SFContactId
import com.gu.sf_contact_merge.getaccounts.GetZuoraContactDetails.FirstName
import com.gu.sf_contact_merge.getsfcontacts.GetSfAddress.SFAddress
import com.gu.sf_contact_merge.getsfcontacts.GetSfAddress.SFAddressFields._
import com.gu.sf_contact_merge.getsfcontacts.GetSfAddressOverride.{DontOverrideAddress, OverrideAddressWith}
import com.gu.sf_contact_merge.update.UpdateSalesforceIdentityId
import com.gu.sf_contact_merge.update.UpdateSalesforceIdentityId._
import com.gu.util.resthttp.RestRequestMaker.{PatchRequest, RelativePath}
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.{JsObject, JsString}

class UpdateSalesforceIdentityIdTest extends FlatSpec with Matchers {

  it should "send the right request for update identity id" in {

    val actual = UpdateSalesforceIdentityId.toRequest(
      SFContactId("contactsf"),
      SFContactUpdate(
        Some(IdentityId("identityid")),
        SetFirstName(FirstName("firstname")),
        OverrideAddressWith(SFAddress(
          SFStreet("street1"),
          Some(SFCity("city1")),
          Some(SFState("state2")),
          Some(SFPostalCode("post1")),
          SFCountry("country1"),
          Some(SFPhone("phone1"))
        ))
      )
    )
    val expectedJson = JsObject(Seq(
      "IdentityID__c" -> JsString("identityid"),
      "FirstName" -> JsString("firstname"),
      "OtherStreet" -> JsString("street1"),
      "OtherCity" -> JsString("city1"),
      "OtherState" -> JsString("state2"),
      "OtherPostalCode" -> JsString("post1"),
      "OtherCountry" -> JsString("country1"),
      "Phone" -> JsString("phone1")
    ))
    val expected = new PatchRequest(expectedJson, RelativePath("/services/data/v43.0/sobjects/Contact/contactsf"))
    actual should be(expected)

  }

  it should "when we update with no first name, should set a dot" in {

    val actual = UpdateSalesforceIdentityId.toRequest(
      SFContactId("contactsf"),
      SFContactUpdate(
        Some(IdentityId("identityid")),
        DummyFirstName,
        DontOverrideAddress
      )
    )
    val expectedJson = JsObject(Seq(
      "IdentityID__c" -> JsString("identityid"),
      "FirstName" -> JsString(".")
    ))
    val expected = new PatchRequest(expectedJson, RelativePath("/services/data/v43.0/sobjects/Contact/contactsf"))
    actual should be(expected)

  }

  it should "when we update with out changing first name, shouldn't change" in {

    val actual = UpdateSalesforceIdentityId.toRequest(
      SFContactId("contactsf"),
      SFContactUpdate(
        Some(IdentityId("identityid")),
        DontChangeFirstName,
        DontOverrideAddress
      )
    )
    val expectedJson = JsObject(Seq(
      "IdentityID__c" -> JsString("identityid")
    ))
    val expected = new PatchRequest(expectedJson, RelativePath("/services/data/v43.0/sobjects/Contact/contactsf"))
    actual should be(expected)

  }

  it should "when try to clean the identity id it sets it to blank" in {

    val actual = UpdateSalesforceIdentityId.toRequest(
      SFContactId("contactsf"),
      SFContactUpdate(
        None,
        DontChangeFirstName,
        DontOverrideAddress
      )
    )
    val expectedJson = JsObject(Seq(
      "IdentityID__c" -> JsString("")
    ))
    val expected = new PatchRequest(expectedJson, RelativePath("/services/data/v43.0/sobjects/Contact/contactsf"))
    actual should be(expected)

  }

}
