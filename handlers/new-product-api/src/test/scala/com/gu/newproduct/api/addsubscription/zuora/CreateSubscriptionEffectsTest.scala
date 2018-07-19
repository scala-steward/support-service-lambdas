package com.gu.newproduct.api.addsubscription.zuora

import java.time.LocalDate

import com.gu.effects.{GetFromS3, RawEffects}
import com.gu.newproduct.api.addsubscription.ZuoraIds.{PlanAndCharge, ProductRatePlanChargeId, ProductRatePlanId}
import com.gu.newproduct.api.addsubscription.zuora.CreateSubscription.WireModel._
import com.gu.newproduct.api.addsubscription.{CaseId, ZuoraAccountId}
import com.gu.test.EffectsTest
import com.gu.util.config.{LoadConfigModule, Stage}
import com.gu.util.resthttp.RestRequestMaker.RequestsPost
import com.gu.util.zuora.{ZuoraRestConfig, ZuoraRestRequestMaker}
import org.scalatest.{FlatSpec, Matchers}
import scalaz.\/-

class CreateSubscriptionEffectsTest extends FlatSpec with Matchers {

  import ZuoraDevContributions._

  it should "create subscription in account" taggedAs EffectsTest in {
    val validCaseIdToAvoidCausingSFErrors = CaseId("5006E000005b5cf")
    val request = CreateSubscription.CreateReq(
      ZuoraAccountId("2c92c0f864a214c30164a8b5accb650b"),
      100,
      LocalDate.now,
      validCaseIdToAvoidCausingSFErrors
    )
    val actual = for {
      zuoraRestConfig <- LoadConfigModule(Stage("DEV"), GetFromS3.fetchString)[ZuoraRestConfig]
      zuoraDeps = ZuoraRestRequestMaker(RawEffects.response, zuoraRestConfig)
      post: RequestsPost[WireCreateRequest, WireSubscription] = zuoraDeps.post[WireCreateRequest, WireSubscription]
      res <- CreateSubscription(monthlyIds, post)(request).toDisjunction
    } yield res
    withClue(actual) {
      actual.map(_.value.substring(0, 3)) shouldBe \/-("A-S")
    }
    // ideally should check that it was really created with the right fields
  }
}

object ZuoraDevContributions {

  val monthlyIds = PlanAndCharge(
    productRatePlanId = ProductRatePlanId("2c92c0f85a6b134e015a7fcd9f0c7855"),
    productRatePlanChargeId = ProductRatePlanChargeId("2c92c0f85a6b1352015a7fcf35ab397c")
  )
}