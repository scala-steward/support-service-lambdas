package com.gu.digitalvouchersuspensionprocessor

import com.gu.digitalvouchersuspensionprocessor.ConfigLoader.Service.Salesforce
import com.gu.digitalvouchersuspensionprocessor.ConfigLoader.{ConfigLoadFailure, Service, loadFromEnvironment, loadFromSecretsManager}
import com.gu.imovo.ImovoConfig
import com.gu.salesforce.SFAuthConfig
import com.gu.util.config.Stage.Dev

case class Config(salesforce: SFAuthConfig, imovo: ImovoConfig)

object Config {

  def fromEnv(): Either[ConfigLoadFailure, Config] =
    for {
      sfSecret <- loadFromSecretsManager(Salesforce)("User/MembersDataAPI")
      sfUrl <- loadFromEnvironment("salesforceUrl")
      sfClientId <- loadFromEnvironment("salesforceClientId")
      sfClientSecret <- loadFromEnvironment("salesforceClientSecret")
      sfUserName <- loadFromEnvironment("salesforceUserName")
      sfPassword <- loadFromEnvironment("salesforcePassword")
      sfToken <- loadFromEnvironment("salesforceToken")
      imovoUrl <- loadFromEnvironment("imovoUrl")
      imovoApiKey <- loadFromEnvironment("imovoApiKey")
    } yield Config(
      salesforce = SFAuthConfig(
        url = sfUrl,
        client_id = sfClientId,
        client_secret = sfClientSecret,
        username = sfUserName,
        password = sfPassword,
        token = sfToken
      ),
      imovo = ImovoConfig(
        imovoBaseUrl = imovoUrl,
        imovoApiKey = imovoApiKey
      )
    )
}
