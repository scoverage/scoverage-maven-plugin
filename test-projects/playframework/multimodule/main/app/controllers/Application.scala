package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index(service.HelloService.hello + ", Your new application is ready."))
  }
  
}