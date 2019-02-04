package com.gu.stripeCustomerSourceUpdated

import com.gu.util.apigateway.ResponseModels.ApiResponse
import com.gu.util.reader.Types.ApiGatewayOp.ContinueProcessing
import com.gu.util.reader.Types._
import com.gu.util.resthttp.Types.{ClientFailableOp, ClientFailure, ClientSuccess}

object TypeConvert {

  implicit class TypeConvertClientOp[A](clientOp: ClientFailableOp[A]) {
    def toApiGatewayOp(action: String): ApiGatewayOp[A] = clientOp.toDisjunction.toApiGatewayOp(action)

    def toApiGatewayOp(failureToApiResponse: ClientFailure => ApiResponse): ApiGatewayOp[A] = clientOp.toDisjunction.toApiGatewayOp(failureToApiResponse)

    def withAmendedError(amendError: ClientFailure => ClientFailure) = clientOp.toDisjunction.leftMap(amendError(_)).toClientFailableOp
  }

}
