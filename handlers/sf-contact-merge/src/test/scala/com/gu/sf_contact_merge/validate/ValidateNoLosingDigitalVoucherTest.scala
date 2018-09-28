package com.gu.sf_contact_merge.validate

import com.gu.sf_contact_merge.getsfcontacts.GetSfContact.IsDigitalVoucherUser
import com.gu.util.reader.Types.ApiGatewayOp.ContinueProcessing
import com.gu.util.resthttp.LazyClientFailableOp
import com.gu.util.resthttp.Types.{ClientSuccess, GenericError}
import org.scalatest.{FlatSpec, Matchers}
import scalaz.-\/

class ValidateNoLosingDigitalVoucherTest extends FlatSpec with Matchers {

  it should "not object to an empty list" in {

    val input: List[LazyClientFailableOp[IsDigitalVoucherUser]] = List()

    val actual = ValidateNoLosingDigitalVoucher(input)

    actual should be(ContinueProcessing(()))
  }

  it should "not object to a couple of falses" in {

    val input: List[LazyClientFailableOp[IsDigitalVoucherUser]] =
      List(
        LazyClientFailableOp(() => ClientSuccess(IsDigitalVoucherUser(false))),
        LazyClientFailableOp(() => ClientSuccess(IsDigitalVoucherUser(false)))
      )

    val actual = ValidateNoLosingDigitalVoucher(input)

    actual should be(ContinueProcessing(()))
  }

  it should "object to a true" in {

    val input: List[LazyClientFailableOp[IsDigitalVoucherUser]] =
      List(
        LazyClientFailableOp(() => ClientSuccess(IsDigitalVoucherUser(true)))
      )

    val actual = ValidateNoLosingDigitalVoucher(input)

    actual.toDisjunction.leftMap(_.statusCode) should be(-\/("404"))
  }

  it should "object to an error" in {

    val input: List[LazyClientFailableOp[IsDigitalVoucherUser]] =
      List(
        LazyClientFailableOp(() => GenericError("test"))
      )

    val actual = ValidateNoLosingDigitalVoucher(input)

    actual.toDisjunction.leftMap(_.statusCode) should be(-\/("500"))
  }

}
