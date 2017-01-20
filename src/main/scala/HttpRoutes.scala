package deeplabs.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

//
// Routes
// 
object Routes {

  val route : Route = 
    path("status") {
      get {
        complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "System-node is OK."))
      }
    }

}
