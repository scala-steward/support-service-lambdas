package com.gu.identityBackfill

import com.gu.identity.GetByEmail
import com.gu.identity.GetByEmail.{NotValidated, ValidatedEmail}
import com.gu.identityBackfill.PreReqCheck.PreReqResult
import com.gu.identityBackfill.Types._
import com.gu.identityBackfill.salesforce.UpdateSalesforceIdentityId.IdentityId
import com.gu.identityBackfill.zuora.GetZuoraSubTypeForAccount.ReaderType
import com.gu.salesforce.TypesForSFEffectsData.SFContactId
import com.gu.util.apigateway.ApiGatewayResponse
import com.gu.util.reader.Types.ApiGatewayOp.{ContinueProcessing, ReturnWithResponse}
import com.gu.util.resthttp.LazyClientFailableOp
import com.gu.util.resthttp.Types.{ClientFailableOp, ClientSuccess, NotFound}
import org.scalatest.{FlatSpec, Matchers}

class PreReqCheckTest extends FlatSpec with Matchers {

  it should "go through a happy case" in {

    val result =
      PreReqCheck(
        email => ClientSuccess(ValidatedEmail(IdentityId("asdf"))),
        email => ContinueProcessing(ZuoraAccountIdentitySFContact(AccountId("acc"), None, SFContactId("sf"))),
        identityId => ContinueProcessing(()),
        _ => ContinueProcessing(()),
        _ => LazyClientFailableOp(() => ClientSuccess(ContinueProcessing(())))
      )(EmailAddress("email@address"))

    val expectedResult = ContinueProcessing(PreReqResult(AccountId("acc"), SFContactId("sf"), Some(IdentityId("asdf"))))
    result should be(expectedResult)
  }

  it should "go through a happy case with no existing identity" in {

    val result =
      PreReqCheck(
        email => NotFound("so we will return None"),
        email => ContinueProcessing(ZuoraAccountIdentitySFContact(AccountId("acc"), None, SFContactId("sf"))),
        identityId => ContinueProcessing(()),
        _ => ContinueProcessing(()),
        _ => LazyClientFailableOp(() => ClientSuccess(ContinueProcessing(())))
      )(EmailAddress("email@address"))

    val expectedResult = ContinueProcessing(PreReqResult(AccountId("acc"), SFContactId("sf"), None))
    result should be(expectedResult)
  }

  it should "stop processing if it finds there is a zuora account already for the identity id" in {

    val result =
      PreReqCheck.noZuoraAccountsForIdentityId(countZuoraAccountsForIdentityId = ClientSuccess(1))

    val expectedResult = ReturnWithResponse(ApiGatewayResponse.notFound("already used that identity id"))
    result should be(expectedResult)
  }

  it should "stop processing if the zuora account for the given email already has an identity id" in {

    val result =
      PreReqCheck.getSingleZuoraAccountForEmail(
        ClientSuccess(List(ZuoraAccountIdentitySFContact(
          AccountId("acc"),
          Some(IdentityId("haha")),
          SFContactId("sf")
        )))
      )

    val expectedResult = ReturnWithResponse(ApiGatewayResponse.notFound("the account we found was already populated with an identity id"))
    result should be(expectedResult)
  }

  it should "stop processing if there are multiple zuora accounts with the same email address" in {

    val result =
      PreReqCheck.getSingleZuoraAccountForEmail({
        val contactWithoutIdentity = ZuoraAccountIdentitySFContact(AccountId("acc"), None, SFContactId("sf"))
        ClientSuccess(List(contactWithoutIdentity, contactWithoutIdentity))
      })

    val expectedResult = ReturnWithResponse(ApiGatewayResponse.notFound("should have exactly one zuora account per email at this stage"))
    result should be(expectedResult)
  }

  it should "stop processing if it finds a non validated identity account" in {

    val result = emailCheckFailure(ClientSuccess(NotValidated))

    val expectedResult = ReturnWithResponse(ApiGatewayResponse.notFound("identity email not validated"))
    result should be(expectedResult)
  }

  private def emailCheckFailure(identityError: ClientFailableOp[GetByEmail.MaybeValidatedEmail]) = {
    PreReqCheck(
      email => identityError,
      email => fail("shouldn't be called 1"),
      identityId => fail("shouldn't be called 2"),
      _ => fail("shouldn't be called 3"),
      _ => fail("shouldn't be called 4")
    )(EmailAddress("email@address"))
  }

  // allow contain either blank or direct, but nothign else
  "acceptable reader type" should "allow blank" in {
    val readerTypes = List(ReaderType.NoReaderType)
    PreReqCheck.acceptableReaderType(ClientSuccess(readerTypes)) should be(ContinueProcessing(()))
  }

  "acceptable reader type" should "allow direct" in {
    val readerTypes = List(ReaderType.ReaderTypeValue("Direct"))
    PreReqCheck.acceptableReaderType(ClientSuccess(readerTypes)) should be(ContinueProcessing(()))
  }

  "acceptable reader type" should "allow multiple valid" in {
    val readerTypes = List(ReaderType.ReaderTypeValue("Direct"), ReaderType.ReaderTypeValue("Direct"))
    PreReqCheck.acceptableReaderType(ClientSuccess(readerTypes)) should be(ContinueProcessing(()))
  }

  "acceptable reader type" should "not allow agent" in {
    val readerTypes = List(ReaderType.ReaderTypeValue("Agent"))
    PreReqCheck.acceptableReaderType(ClientSuccess(readerTypes)).toDisjunction.leftMap(_.statusCode) should be(scalaz.-\/("404"))
  }

  "acceptable reader type" should "not allow gift" in {
    val readerTypes = List(ReaderType.ReaderTypeValue("Gift"))
    PreReqCheck.acceptableReaderType(ClientSuccess(readerTypes)).toDisjunction.leftMap(_.statusCode) should be(scalaz.-\/("404"))
  }

  "acceptable reader type" should "not allow a combination of valid and invalid" in {
    val readerTypes = List(ReaderType.ReaderTypeValue("Direct"), ReaderType.ReaderTypeValue("Gift"))
    PreReqCheck.acceptableReaderType(ClientSuccess(readerTypes)).toDisjunction.leftMap(_.statusCode) should be(scalaz.-\/("404"))
  }

}
