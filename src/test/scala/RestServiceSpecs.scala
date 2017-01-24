package deeplabs.http


import akka.http.scaladsl.testkit._
import org.scalatest._
import akka.stream.scaladsl.FileIO
import akka.http.scaladsl.server._
import akka.http.scaladsl.model._
import HttpMethods._
import Directives._

import java.nio.file._

class RestServiceSpec extends WordSpec with Matchers with ScalatestRouteTest with RestService {

  "Status API" should {
    "Http Get to /status should see a message 'System-node is OK.'" in {
      Get("/status") ~> routes ~> check {
        status.isSuccess() shouldEqual true
      }
    }
  }

  "Image upload API" should {
    "Http Post image to /video should be successful." in {
      Post("/video", HttpEntity(MediaTypes.`image/jpeg`, FileIO.fromPath(Paths.get(this.getClass.getClassLoader().getResource("bigimage.jpg").toURI())))) ~> routes ~> check {
        status.isSuccess() shouldEqual true
      }
    }
  }

}


