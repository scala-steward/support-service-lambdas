package com.com.gu.sf_datalake_export.handlers

import java.time.LocalDate

import com.gu.sf_datalake_export.handlers.StartJobHandler
import com.gu.sf_datalake_export.handlers.StartJobHandler.{UploadToDataLake, WireResponse}
import com.gu.sf_datalake_export.salesforce_bulk_api.AddQueryToJob.AddQueryRequest
import com.gu.sf_datalake_export.salesforce_bulk_api.BulkApiParams.{BatchSize, ObjectName, SfObjectName, Soql}
import com.gu.sf_datalake_export.salesforce_bulk_api.CreateJob.{CreateJobRequest, JobId}
import com.gu.sf_datalake_export.salesforce_bulk_api.SfQueries
import com.gu.util.config.Stage
import com.gu.util.handlers.LambdaException
import com.gu.util.resthttp.Types.ClientSuccess
import org.scalatest.{FlatSpec, Matchers}

import scala.util.parsing.combinator.Parsers
import scala.util.{Failure, Success}

class StartJobStepTest extends FlatSpec with Matchers {

  def createJob(req: CreateJobRequest) = {
    req.maybeChunkSize shouldBe Some(BatchSize(250000))
    req.objectType shouldBe SfObjectName("Contact")
    ClientSuccess(JobId("someJobId"))
  }

  def addQuery(req: AddQueryRequest) = {
    req.jobId shouldBe JobId("someJobId")
    req.query shouldBe Soql(SfQueries.contactQuery)
    ClientSuccess(())
  }

  val today = () => LocalDate.of(2018, 10, 22)

  val testSteps = StartJobHandler.steps(today, createJob, addQuery) _

  "startJob.steps" should "create a job and add the correct query" in {

    val expectedResponse = WireResponse(
      jobId = "someJobId",
      objectName = "Contact",
      jobName = "Contact_2018-10-22",
      uploadToDataLake = false
    )
    testSteps(ObjectName("Contact"), UploadToDataLake(false)) shouldBe Success(expectedResponse)
  }

  it should "return failure if object in request is unknown" in {
    testSteps(ObjectName("unknownObject"), UploadToDataLake(false)) shouldBe Failure(LambdaException("invalid object name unknownObject"))
  }

  "UploadToDataLake" should "be set to true by default in PROD" in {
    UploadToDataLake(None, Stage("PROD")) shouldBe Success(UploadToDataLake(true))
  }
  it should "be set to true by default in non PROD stages" in {
    UploadToDataLake(None, Stage("NOT-PROD")) shouldBe Success(UploadToDataLake(false))
  }

  it should "return an error if attempted to set to true in non PROD ENV" in {
    UploadToDataLake(Some(true), Stage("NOT-PROD")) shouldBe Failure(LambdaException("uploadToDatalake can only be enabled in PROD"))
  }

  it should "should be set to the correct value in PROD" in {
    val actualTrue = UploadToDataLake(Some(true), Stage("PROD"))
    val actualFalse = UploadToDataLake(Some(false), Stage("PROD"))
    val expectedResponses = (trueResponse, falseResponse)
    (actualTrue, actualFalse) shouldBe expectedResponses

  }

  val trueResponse = Success(UploadToDataLake(true))
  val falseResponse = Success(UploadToDataLake(false))
}
