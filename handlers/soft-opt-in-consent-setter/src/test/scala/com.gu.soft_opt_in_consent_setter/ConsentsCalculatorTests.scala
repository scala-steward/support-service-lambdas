package com.gu.soft_opt_in_consent_setter

import com.gu.soft_opt_in_consent_setter.models.SoftOptInError
import com.gu.soft_opt_in_consent_setter.testData.Consents.{calculator, contributionMapping, guWeeklyMapping, membershipMapping, newspaperMapping}
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class ConsentsCalculatorTests extends AnyFlatSpec with should.Matchers with EitherValues {

  // getAcqConsents success cases
  "getAcqConsents" should "correctly return the mapping when a known product is passed" in {
    calculator.getAcqConsents("membership") shouldBe Right(membershipMapping)
  }

  // getAcqConsents failure cases
  "getAcqConsents" should "correctly return a SoftOptInError when the product isn't present in the mappings" in {
    val result = calculator.getAcqConsents("nonexistentProduct")

    result.isLeft shouldBe true
    result.left.value shouldBe a[SoftOptInError]
    result.left.value.errorType shouldBe "ConsentsCalculator"
  }

  // getCancConsents success cases
  "getCancConsents" should "correctly return the mapping when a known product is passed and there are no owned products" in {
    calculator.getCancConsents("membership", Set()) shouldBe Right(membershipMapping)
  }

  "getCancConsents" should "correctly return the mapping when a known product is passed and there are owned products but do not overlap" in {
    calculator.getCancConsents("membership", Set("testproduct")) shouldBe Right(membershipMapping)
  }

  "getCancConsents" should "correctly return the mapping when a known product is passed and there is an owned product that partially overlaps" in {
    calculator.getCancConsents("newspaper", Set("guardianweekly")) shouldBe Right(newspaperMapping.diff(guWeeklyMapping))
  }

  "getCancConsents" should "correctly return the mapping when a known product is passed and there are multiple owned products that partially overlap" in {
    calculator.getCancConsents("newspaper", Set("membership", "guardianweekly")) shouldBe Right(newspaperMapping.diff(membershipMapping ++ guWeeklyMapping))
  }

  "getCancConsents" should "correctly return the mapping when a known product is passed and there is an owned products completely overlaps" in {
    calculator.getCancConsents("guardianweekly", Set("membership")) shouldBe Right(guWeeklyMapping.diff(membershipMapping))
  }

  "getCancConsents" should "correctly return the mapping when a known product is passed and there are multiple owned products that completely overlap" in {
    calculator.getCancConsents("guardianweekly", Set("membership", "contributions")) shouldBe Right(guWeeklyMapping.diff(membershipMapping ++ contributionMapping))
  }

  // getCancConsents failure cases
  "getCancConsents" should "correctly return a SoftOptInError when a unknown product is passed and there are no owned products" in {
    val result = calculator.getCancConsents("nonexistentProduct", Set())

    result.isLeft shouldBe true
    result.left.value shouldBe a[SoftOptInError]
    result.left.value.errorType shouldBe "ConsentsCalculator"
  }

  "getCancConsents" should "correctly return a SoftOptInError when a known product is passed and an unknown product is present in the owned products" in {
    val result = calculator.getCancConsents("membership", Set("nonexistentProduct"))

    result.isLeft shouldBe true
    result.left.value shouldBe a[SoftOptInError]
    result.left.value.errorType shouldBe "ConsentsCalculator"
  }

  // buildConsentsBody success cases
  "buildConsentsBody" should "return an empty JSON array when consents is empty" in {
    removeWhitespace(calculator.buildConsentsBody(Set(), true)) shouldBe removeWhitespace("""[]""".stripMargin)
  }

  "buildConsentsBody" should "return a correctly populated JSON array when consents is not empty and state is true" in {
    removeWhitespace(calculator.buildConsentsBody(guWeeklyMapping, true)) shouldBe
      removeWhitespace("""[
    |  {
    |    "id" : "your_support_onboarding",
    |    "consented" : true
    |  },
    |  {
    |    "id" : "guardian_weekly_newsletter",
    |    "consented" : true
    |  }
    |]""".stripMargin)
  }

  "buildConsentsBody" should "return a correctly populated JSON array when consents is not empty and state is false" in {
    removeWhitespace(calculator.buildConsentsBody(guWeeklyMapping, false)) shouldBe
      removeWhitespace("""[
    |  {
    |    "id" : "your_support_onboarding",
    |    "consented" : false
    |  },
    |  {
    |    "id" : "guardian_weekly_newsletter",
    |    "consented" : false
    |  }
    |]""".stripMargin)
  }

  def removeWhitespace(stringToRemoveWhitespaceFrom: String): String = {
    stringToRemoveWhitespaceFrom.replaceAll("\\s", "")
  }
}

