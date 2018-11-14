package com.com.gu.sf_datalake_export

import java.time.LocalDate

import com.gu.sf_datalake_export.StartJob
import com.gu.sf_datalake_export.StartJob.{WireRequest, WireResponse}
import com.gu.sf_datalake_export.salesforce_bulk_api.AddQueryToJob.AddQueryRequest
import com.gu.sf_datalake_export.salesforce_bulk_api.BulkApiParams.{BatchSize, SfObjectName, Soql}
import com.gu.sf_datalake_export.salesforce_bulk_api.CreateJob.{CreateJobRequest, JobId}
import com.gu.sf_datalake_export.salesforce_bulk_api.SfQueries
import com.gu.util.handlers.LambdaException
import com.gu.util.resthttp.Types.ClientSuccess
import org.scalatest.{FlatSpec, Matchers}

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

  def today() = LocalDate.of(2018, 10, 22)

  val testSteps = StartJob.steps(today, createJob, addQuery) _

  "startJob.steps" should "create a job and add the correct query" in {

    val expectedResponse = WireResponse(
      jobId = "someJobId",
      jobName = "Contact_2018-10-22"
    )
    testSteps(WireRequest("Contact")) shouldBe Success(expectedResponse)
  }

  it should "return failure if object in request is unknown" in {
    testSteps(WireRequest("unknownObject")) shouldBe Failure(LambdaException("invalid object name unknownObject"))
  }


}
