package com.gu.digitalvouchersuspensionprocessor

import com.gu.digitalvouchersuspensionprocessor.ConfigLoader.Service.Salesforce
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest

import scala.util.Try

object ConfigLoader {

  case class ConfigLoadFailure(reason: String)

  sealed trait Service { def name: String }
  object Service {
    case object Salesforce extends Service { val name = "Salesforce" }
  }

  def loadFromEnvironment(name: String): Either[ConfigLoadFailure, String] =
    sys.env.get(name).toRight(ConfigLoadFailure(s"No value in environment for '$name'"))

  private lazy val secretsManagerClient = SecretsManagerClient.create()

  private lazy val stage = loadFromEnvironment("Stage")

  def loadFromSecretsManager(service: Service)(name: String): Either[ConfigLoadFailure, String] = {
    for {
      stageName <- stage
      request = GetSecretValueRequest.builder.secretId(s"$stageName/${service.name}/$name").build()
      response <- Try(secretsManagerClient.getSecretValue(request))
        .toEither
        .left
        .map(e => ConfigLoadFailure(e.getMessage))
    } yield response.secretString
  }
}

// TODO remove
object Client extends App {
  val name = "User/MembersDataAPI"
  val x = ConfigLoader.loadFromSecretsManager(Salesforce)(name)
  x match {
    case Left(e) => println(s"Failed to fetch '$name' from secrets: $e")
    case Right(s) => println(s"secret: $s")
  }
}
