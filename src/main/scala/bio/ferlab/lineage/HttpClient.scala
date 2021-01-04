package bio.ferlab.lineage

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, _}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.Materializer
import spray.json.{JsonWriter, enrichAny}

import scala.concurrent.{ExecutionContext, Future}

object HttpClient {

  case class DefaultErrorResponse(statusCode: Int, message: String)

  def unmarshalTo[T](response: Future[HttpResponse])
                    (implicit ec: ExecutionContext, mat: Materializer, um: Unmarshaller[ResponseEntity, T]): Future[Either[DefaultErrorResponse, T]] = {
    response
      .flatMap {
        case v if v.status.isSuccess() => Unmarshal(v.entity).to[T].map(r => Right(r).asInstanceOf[Either[DefaultErrorResponse, T]])
        case error => Future.successful(Left(DefaultErrorResponse(error.status.intValue(), error.status.reason())))
      }
  }

  def PUT[REQUEST, RESPONSE](uri: String, body: REQUEST)
                  (implicit ec: ExecutionContext, system: ActorSystem[Nothing], mat: Materializer,
                   jw: JsonWriter[REQUEST], um: Unmarshaller[ResponseEntity, RESPONSE])
  : Future[Either[DefaultErrorResponse, RESPONSE]] = {
    val request =
      HttpRequest(
        method = HttpMethods.PUT,
        uri = uri,
        entity = HttpEntity(ContentTypes.`application/json`, body.toJson.toString())
      )
    unmarshalTo[RESPONSE](Http().singleRequest(request))
  }

  def GET[RESPONSE](uri: String)
                   (implicit ec: ExecutionContext, system: ActorSystem[Nothing],
                    um: Unmarshaller[ResponseEntity, RESPONSE])
  : Future[Either[DefaultErrorResponse, RESPONSE]] = {
    unmarshalTo[RESPONSE](Http().singleRequest(HttpRequest(uri = uri, method = HttpMethods.GET)))
  }

}
