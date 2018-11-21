package com.manual_test

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.gu.sf_datalake_export.handlers.EndJobHandler

//This is just a way to locally run the lambda in dev
object EndJobManualTest extends App {
  val request =
    """{
      | "jobId" : "7506E000004FloxQAC"
      |}
    """.stripMargin

  println(s"sending request..")
  println(request)

  val testInputStream = new ByteArrayInputStream(request.getBytes)
  val testOutput = new ByteArrayOutputStream()
  EndJobHandler(testInputStream, testOutput, null)

  val response = new String(testOutput.toByteArray)
  println(response)
}