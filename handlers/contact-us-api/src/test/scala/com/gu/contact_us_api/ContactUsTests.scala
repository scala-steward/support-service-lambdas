package com.gu.contact_us_api

import com.gu.contact_us_api.models.{ContactUsFailureResponse, ContactUsSuccessfulResponse}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class ContactUsTests extends AnyFlatSpec with should.Matchers {
  val errorMsg = "error"
  val SFConnector = new SalesforceConnector()

  // TODO: Turn ContactUs.buildResponse back to protected/private and test processRequest instead

  "ContactUs.buildResponse" should "return a ContactUsSuccessfulResponse on Success" in {
    (new ContactUs(SFConnector)).buildResponse(Right(())) shouldBe Right(ContactUsSuccessfulResponse())
  }

  it should "return a ContactUsFailureResponse on Throwable" in {
    (new ContactUs(SFConnector)).buildResponse(Left(new Throwable(errorMsg))) shouldBe Left(ContactUsFailureResponse(errorMsg))
  }
}