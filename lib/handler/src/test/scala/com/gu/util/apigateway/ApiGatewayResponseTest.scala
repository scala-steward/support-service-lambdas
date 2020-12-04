package com.gu.util.apigateway

import com.gu.util.apigateway.ResponseWriters._
import matchers.should.Matchers._
import play.api.libs.json.Json
import org.scalatest.matchers
import org.scalatest.flatspec.AnyFlatSpec

class ApiGatewayResponseTest extends AnyFlatSpec {

  "ApiGatewayResponse" should "serialise success response" in {
    val data = ApiGatewayResponse.successfulExecution
    val expected =
      """{
        |"statusCode":"200","headers":{"Content-Type":"application/json"},"body":"{\n  \"message\" : \"Success\"\n}"
        |}
      """.stripMargin
    val result = Json.toJson(data)
    result should be(Json.parse(expected))
  }

}
