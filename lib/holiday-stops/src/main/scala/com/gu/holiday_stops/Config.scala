package com.gu.holiday_stops

import com.gu.holiday_stops.config._
import com.gu.salesforce.SalesforceAuthenticate.SFAuthConfig
import com.gu.util.config.LoadConfigModule.StringFromS3
import com.gu.util.config.{ConfigLocation, LoadConfigModule, Stage}
import play.api.libs.json.{Json, Reads}

case class Config(
  zuoraConfig: ZuoraConfig,
  sfConfig: SFAuthConfig,
  holidayCreditProduct: HolidayCreditProduct,
  guardianWeeklyConfig: GuardianWeeklyHolidayStopConfig,
  saturdayVoucherConfig: SaturdayVoucherHolidayStopConfig,
  sundayVoucherConfig: SundayVoucherHolidayStopConfig,
  weekendVoucherConfig: WeekendVoucherHolidayStopConfig,
  sixdayVoucherConfig: SixdayVoucherHolidayStopConfig,
  everydayVoucherConfig: EverydayVoucherHolidayStopConfig,
  everydayPlusVoucherConfig: EverydayPlusVoucherHolidayStopConfig,
  sixdayPlusVoucherConfig: SixdayPlusVoucherHolidayStopConfig,
  weekendPlusVoucherConfig: WeekendPlusVoucherHolidayStopConfig,
  sundayPlusVoucherConfig: SundayPlusVoucherHolidayStopConfig,
  saturdayPlusVoucherConfig: SaturdayPlusVoucherHolidayStopConfig,
)

case class ZuoraConfig(
  baseUrl: String,
  holidayStopProcessor: HolidayStopProcessor
)

case class HolidayStopProcessor(oauth: Oauth)

case class Oauth(clientId: String, clientSecret: String)

object Config {

  implicit val oAuthØReads = Json.reads[Oauth]
  implicit val holidayStopProcessorReads = Json.reads[HolidayStopProcessor]
  implicit val sfAuthConfigReads = Json.reads[SFAuthConfig]
  implicit val zuoraConfigReads = Json.reads[ZuoraConfig]

  private def zuoraCredentials(stage: String, fetchString: StringFromS3): Either[OverallFailure, ZuoraConfig] = {
    credentials[ZuoraConfig](stage, "zuoraRest", fetchString)
  }

  private def salesforceCredentials(stage: String, fetchString: StringFromS3): Either[OverallFailure, SFAuthConfig] = {
    credentials[SFAuthConfig](stage, "sfAuth", fetchString)
  }

  private def credentials[T](stage: String, filePrefix: String, fetchString: StringFromS3)(implicit reads: Reads[T]): Either[OverallFailure, T] = {
    val loadConfigModule = LoadConfigModule(Stage(stage), fetchString)
    loadConfigModule
      .apply[T](ConfigLocation(filePrefix, 1), reads)
      .leftMap(failure => OverallFailure(failure.error))
      .toEither
  }

  def apply(fetchString: StringFromS3): Either[OverallFailure, Config] = {
    val stage = Option(System.getenv("Stage")).getOrElse("DEV")
    for {
      zuoraConfig <- zuoraCredentials(stage, fetchString)
      sfConfig <- salesforceCredentials(stage, fetchString)
    } yield {
      stage match {
        case "PROD" =>
          Config(
            zuoraConfig,
            sfConfig,
            HolidayCreditProduct.Prod,
            GuardianWeeklyHolidayStopConfig.Prod,
            SaturdayVoucherHolidayStopConfig.Prod,
            SundayVoucherHolidayStopConfig.Prod,
            WeekendVoucherHolidayStopConfig.Prod,
            SixdayVoucherHolidayStopConfig.Prod,
            EverydayVoucherHolidayStopConfig.Prod,
            EverydayPlusVoucherHolidayStopConfig.Prod,
            SixdayPlusVoucherHolidayStopConfig.Prod,
            WeekendPlusVoucherHolidayStopConfig.Prod,
            SundayPlusVoucherHolidayStopConfig.Prod,
            SaturdayPlusVoucherHolidayStopConfig.Prod,
          )
        case "CODE" =>
          Config(
            zuoraConfig,
            sfConfig,
            HolidayCreditProduct.Code,
            GuardianWeeklyHolidayStopConfig.Code,
            SaturdayVoucherHolidayStopConfig.Code,
            SundayVoucherHolidayStopConfig.Code,
            WeekendVoucherHolidayStopConfig.Code,
            SixdayVoucherHolidayStopConfig.Code,
            EverydayVoucherHolidayStopConfig.Code,
            EverydayPlusVoucherHolidayStopConfig.Code,
            SixdayPlusVoucherHolidayStopConfig.Code,
            WeekendPlusVoucherHolidayStopConfig.Code,
            SundayPlusVoucherHolidayStopConfig.Code,
            SaturdayPlusVoucherHolidayStopConfig.Code,
          )
        case "DEV" =>
          Config(
            zuoraConfig,
            sfConfig,
            HolidayCreditProduct.Dev,
            GuardianWeeklyHolidayStopConfig.Dev,
            SaturdayVoucherHolidayStopConfig.Dev,
            SundayVoucherHolidayStopConfig.Dev,
            WeekendVoucherHolidayStopConfig.Dev,
            SixdayVoucherHolidayStopConfig.Dev,
            EverydayVoucherHolidayStopConfig.Dev,
            EverydayPlusVoucherHolidayStopConfig.Dev,
            SixdayPlusVoucherHolidayStopConfig.Dev,
            WeekendPlusVoucherHolidayStopConfig.Dev,
            SundayPlusVoucherHolidayStopConfig.Dev,
            SaturdayPlusVoucherHolidayStopConfig.Dev,
          )
      }
    }
  }
}
