package com.gu.salesforce

import com.gu.util.config.ConfigLocation

case class SFAuthConfig(
  url: String,
  client_id: String,
  client_secret: String,
  username: String,
  password: String,
  token: String
)

object SFAuthConfig {
  implicit val location = ConfigLocation[SFAuthConfig](path = "sfAuth", version = 1)
}

object SFAuthTestConfig {
  implicit val location = ConfigLocation[SFAuthConfig](path = "TEST/sfAuth", version = 1)
}

object SFExportAuthConfig {
  val location = ConfigLocation[SFAuthConfig](path = "sfExportAuth", version = 1)
}