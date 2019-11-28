package com.gu.supporter.fulfilment

import com.amazonaws.services.lambda.runtime.Context
import com.typesafe.scalalogging.LazyLogging
import io.github.mkotsur.aws.handler.Lambda
import io.circe.generic.auto._
import io.circe.parser._
import io.github.mkotsur.aws.handler.Lambda._

class FulfilmentDateCalculator extends Lambda[String, String] with LazyLogging {
  override def handle(todayOverride: String, context: Context) = {
    Right(todayOverride)
  }
}
