package com.gu.payment_failure_comms.models

case class BrazeTrackRequest(events: List[CustomEvent])

// Based on https://www.braze.com/docs/api/objects_filters/event_object/
case class CustomEvent(external_id: String, app_id: String, name: String, time: String, properties: Map[String, String])
