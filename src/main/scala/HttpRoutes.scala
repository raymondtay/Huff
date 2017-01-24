package deeplabs.http

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.scaladsl.FileIO
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import deeplabs.http.json.DLLog
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import com.typesafe.scalalogging.Logger
//
// Routes
// 
// The following http routes are listed here:
// GET  `/status` - get the status of the current http node
// POST `/video`  - post image data
// 
case class Routes(implicit val actorMaterializer: ActorMaterializer) {

  import scala.concurrent.ExecutionContext.Implicits.global
 
  val logger = Logger(Routes.getClass)

  val data = DLLog(
      service_name = "Huff Http Cluster",
      category     = "application",
      event_type   = "operation",
      message      = ""
      )
 
  val route : Route = 
  (get & path("status")) {
    get {
      val d = data.copy(message = "URI: /status invoked. Ok.")
      logger.info(d.asJson.noSpaces)
      complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "System-node is OK."))
    }
  } ~ 
  (post & path("video")) {
    withoutSizeLimit {
      extractDataBytes { 
        bytes ⇒ 
          val fileWrite = bytes.runWith(FileIO.toPath(new File("/tmp/test.jpg").toPath))
          fileWrite.onFailure {
            case e ⇒ 
              val d = data.copy(message = s"URI: /video invoked. Uploaded failed with exception: ${e.getMessage}")
              logger.info(d.asJson.noSpaces)
          } 
 
          onComplete(fileWrite) { ioResult ⇒ 
            val d = data.copy(message = "URI: /video invoked. Uploaded Ok.")
            logger.info(d.asJson.noSpaces)
            complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"File IO ${ioResult} uploaded."))
          }
         
     }
    }
  }

}

