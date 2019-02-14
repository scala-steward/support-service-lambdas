package com.gu.newproduct.api

import com.gu.effects.sqs.AwsSQSSend.QueueName
import com.gu.util.config.Stage

case class EmailQueueNames(contributions: QueueName, paper: QueueName)

object EmailQueueNames {
  def emailQueuesFor(stage: Stage) = stage match {
    case Stage("PROD") | Stage("CODE") => EmailQueueNames(contributions = QueueName("contributions-thanks"), paper = QueueName("subs-welcome-email"))
    case _ => EmailQueueNames(contributions = QueueName("contributions-thanks-dev"), paper = QueueName("subs-welcome-email-dev"))
  }
}
