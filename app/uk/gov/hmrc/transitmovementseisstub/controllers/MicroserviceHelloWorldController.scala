package uk.gov.hmrc.transitmovementseisstub.controllers

import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton()
class MicroserviceHelloWorldController @Inject() (cc: ControllerComponents) extends BackendController(cc) {

  def hello(): Action[AnyContent] = Action.async {
    Future.successful(Ok("Hello world"))
  }
}
