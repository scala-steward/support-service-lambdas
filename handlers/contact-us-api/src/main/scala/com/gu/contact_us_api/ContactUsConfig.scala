package com.gu.contact_us_api

object ContactUsConfig {
  // TODO: Obtain these from AWS instead
  val clientID: String = System.getenv("CUClientID")
  val clientSecret: String = System.getenv("CUClientSecret")
  val username: String = System.getenv("CUUsername")
  val password: String = System.getenv("CUPassword")
  val token: String = System.getenv("CUToken")

  val authEndpoint = "https://test.salesforce.com/services/oauth2/token"
  val reqEndpoint = "https://gnmtouchpoint--DEV.my.salesforce.com/services/data/v43.0/composite/"
}