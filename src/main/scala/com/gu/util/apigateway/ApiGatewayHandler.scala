package com.gu.util.apigateway

import java.io.{ InputStream, OutputStream }

import com.amazonaws.services.lambda.runtime.Context
import com.gu.util.Auth.credentialsAreValid
import com.gu.util._
import com.gu.util.apigateway.ApiGatewayResponse.{ outputForAPIGateway, successfulExecution, unauthorized }
import com.gu.util.apigateway.ResponseModels.ApiResponse
import com.gu.util.reader.Types._
import okhttp3.{ Request, Response }
import play.api.libs.json.Json

import scala.io.Source
import scala.util.Try
import scalaz.{ -\/, \/, \/- }

object ApiGatewayHandler extends Logging {

  //  code dependencies
  case class HandlerDeps(
    s3Load: () => Try[String],
    stage: Stage,
    parseConfig: String => Try[Config],
    operation: Config => ApiGatewayRequest => FailableOp[Unit]
  )

  object HandlerDeps {
    def default(stage: Stage, s3Load: Stage => Try[String], operation: Config => ApiGatewayRequest => FailableOp[Unit]): HandlerDeps =
      HandlerDeps(
        () => s3Load(stage),
        stage,
        Config.parseConfig,
        operation
      )
  }

  def apply(deps: HandlerDeps)(inputStream: InputStream, outputStream: OutputStream, context: Context): Unit = {

    val response = for {
      config <- loadConfig(deps)
      apiGatewayRequest <- parseApiGatewayRequest(inputStream)
      _ <- authenticateCallout(apiGatewayRequest.requestAuth, config.trustedApiConfig).withLogging("authentication")
      _ <- deps.operation(config)(apiGatewayRequest)
    } yield ()

    outputForAPIGateway(outputStream, response.fold(identity, _ => successfulExecution))

  }

  def loadConfig(deps: HandlerDeps): FailableOp[Config] = {
    val stage = deps.stage
    logger.info(s"${this.getClass} Lambda is starting up in $stage")

    for {

      textConfig <- deps.s3Load().toFailableOp("load config from s3")
      config <- deps.parseConfig(textConfig).toFailableOp("parse config file")
      _ <- if (stage == config.stage) { \/-(()) } else { -\/(ApiGatewayResponse.internalServerError(s"running in $stage with config from ${config.stage}")) }

    } yield config
  }

  def parseApiGatewayRequest(inputStream: InputStream): FailableOp[ApiGatewayRequest] = {
    for {

      jsonString <- inputFromApiGateway(inputStream).toFailableOp("get json data from API gateway").withLogging("payload from api gateway")
      apiGatewayRequest <- Json.parse(jsonString).validate[ApiGatewayRequest].toFailableOp.withLogging("parsed api gateway object")

    } yield apiGatewayRequest
  }

  private def inputFromApiGateway(inputStream: InputStream) = {
    Try {
      Source.fromInputStream(inputStream).mkString
    }
  }

  def authenticateCallout(requestAuth: Option[RequestAuth], trustedApiConfig: TrustedApiConfig): ApiResponse \/ Unit = {
    if (credentialsAreValid(requestAuth, trustedApiConfig)) \/-(()) else -\/(unauthorized)
  }

}