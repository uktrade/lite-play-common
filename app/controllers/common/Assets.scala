package controllers.common

import javax.inject.{Inject, Singleton}

import controllers.AssetsBuilder
import play.api.http.HttpErrorHandler

@Singleton
class Assets @Inject() (errorHandler: HttpErrorHandler) extends AssetsBuilder(errorHandler)