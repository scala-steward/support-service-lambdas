package com.gu.sf_contact_merge

import com.gu.sf_contact_merge.GetZuoraEmailsForAccounts.EmailAddress
import org.scalatest.{FlatSpec, Matchers}

class AssertSameEmailsTest extends FlatSpec with Matchers {

  it should "be all ok with the same email addresses" in {
    val testData = List("hi", "hi").map(email => Some(EmailAddress(email)))

    AssertSameEmails(testData).toDisjunction.isRight should be(true)

  }

  it should "complain with different email addresses" in {
    val testData = List("hi", "lo").map(email => Some(EmailAddress(email)))

    AssertSameEmails(testData).toDisjunction.isRight should be(false)

  }

  it should "complain with no email address" in {
    val testData = List(Some("hi"), None).map(_.map(EmailAddress.apply))

    AssertSameEmails(testData).toDisjunction.isRight should be(false)

  }

}