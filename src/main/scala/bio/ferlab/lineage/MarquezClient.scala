package bio.ferlab.lineage

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.Materializer
import bio.ferlab.lineage.MarquezClient._

import spray.json.{DefaultJsonProtocol, enrichAny}

import scala.concurrent.{ExecutionContext, Future}

class MarquezClient(baseUrl: String = "http://localhost:5000") extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val defaultErrorResponseFormat = jsonFormat2(DefaultErrorResponse)

  implicit val namespaceRequestFormat = jsonFormat2(NamespaceRequest)
  implicit val namespaceResponseFormat = jsonFormat5(NamespaceResponse)
  implicit val listNamespacesResponseFormat = jsonFormat1(ListNamespacesResponse)

  implicit val sourceRequestFormat = jsonFormat3(SourceRequest)
  implicit val sourceResponseFormat = jsonFormat6(SourceResponse)
  implicit val listSourcesResponseFormat = jsonFormat1(ListSourcesResponse)

  implicit val datasetFieldFormat = jsonFormat4(DatasetField)
  implicit val datasetIdFormat = jsonFormat2(DatasetId)
  implicit val datasetRequestFormat = jsonFormat5(DatasetRequest)
  implicit val datasetResponseFormat = jsonFormat12(DatasetResponse)
  implicit val listDatasetsResponseFormat = jsonFormat1(ListDatasetsResponseFormat)


  val apiV1Url = s"$baseUrl/api/v1"
  val namespacesUrl = s"$apiV1Url/namespaces"
  val sourcesUrl = s"$apiV1Url/sources"
  val tagsUrl = s"$apiV1Url/tags"

  def createDataset(namespace: String, name: String, body: DatasetRequest)
                   (implicit ec: ExecutionContext, system: ActorSystem[Nothing]):
  Future[Either[DefaultErrorResponse, DatasetResponse]] = {
    val request =
      HttpRequest(
        method = HttpMethods.PUT,
        uri = s"$namespacesUrl/$namespace/datasets/$name",
        entity = HttpEntity(ContentTypes.`application/json`, body.toJson.toString())
      )
    unmarshalTo[DatasetResponse](Http().singleRequest(request))
  }

  def getDataset(namespace: String, name: String)
                (implicit ec: ExecutionContext, system: ActorSystem[Nothing]): Future[Either[DefaultErrorResponse, DatasetResponse]] = {
    unmarshalTo[DatasetResponse](
      Http().singleRequest(HttpRequest(uri = s"$namespacesUrl/$namespace/datasets/$name", method = HttpMethods.GET)))
  }

  def getDatasets(namespace: String)(implicit ec: ExecutionContext, system: ActorSystem[Nothing]): Future[Either[DefaultErrorResponse, ListDatasetsResponseFormat]] = {
    unmarshalTo[ListDatasetsResponseFormat](
      Http().singleRequest(HttpRequest(uri = s"$namespacesUrl/$namespace/datasets", method = HttpMethods.GET)))
  }

  def createNamespace(name: String, body: NamespaceRequest)
                     (implicit ec: ExecutionContext, system: ActorSystem[Nothing]):
  Future[Either[DefaultErrorResponse, NamespaceResponse]] = {
    val request =
      HttpRequest(
        method = HttpMethods.PUT,
        uri = s"$namespacesUrl/$name",
        entity = HttpEntity(ContentTypes.`application/json`, body.toJson.toString())
      )
    unmarshalTo[NamespaceResponse](Http().singleRequest(request))
  }

  def getNamespace(name: String)
                  (implicit ec: ExecutionContext, system: ActorSystem[Nothing]): Future[Either[DefaultErrorResponse, NamespaceResponse]] = {
    unmarshalTo[NamespaceResponse](Http().singleRequest(HttpRequest(uri = s"$namespacesUrl/$name", method = HttpMethods.GET)))
  }

  def getNamespaces()(implicit ec: ExecutionContext, system: ActorSystem[Nothing]): Future[Either[DefaultErrorResponse, ListNamespacesResponse]] = {
    unmarshalTo[ListNamespacesResponse](Http().singleRequest(HttpRequest(uri = s"$namespacesUrl/", method = HttpMethods.GET)))
  }

  def createSource(name: String, body: SourceRequest)
                  (implicit ec: ExecutionContext, system: ActorSystem[Nothing]):
  Future[Either[DefaultErrorResponse, SourceResponse]] = {
    val request =
      HttpRequest(
        method = HttpMethods.PUT,
        uri = s"$sourcesUrl/$name",
        entity = HttpEntity(ContentTypes.`application/json`, body.toJson.toString())
      )
    unmarshalTo[SourceResponse](Http().singleRequest(request))
  }

  def getSource(name: String)
               (implicit ec: ExecutionContext, system: ActorSystem[Nothing]):
  Future[Either[DefaultErrorResponse, SourceResponse]] = {
    unmarshalTo[SourceResponse](
      Http().singleRequest(HttpRequest(uri = s"$sourcesUrl/$name", method = HttpMethods.GET)))
  }

  def getSources()
                (implicit ec: ExecutionContext, system: ActorSystem[Nothing]):
  Future[Either[DefaultErrorResponse, ListSourcesResponse]] = {
    unmarshalTo[ListSourcesResponse](
      Http().singleRequest(HttpRequest(uri = s"$sourcesUrl/", method = HttpMethods.GET)))
  }
}

object MarquezClient {
  case class DefaultErrorResponse(statusCode: Int, message: String)

  case class NamespaceRequest(ownerName: String,
                              description: String)

  case class NamespaceResponse(name: String,
                               createdAt: String,
                               updatedAt: String,
                               ownerName: String,
                               description: String)

  case class ListNamespacesResponse(namespaces: List[NamespaceResponse])

  case class SourceRequest(`type`: String,
                           connectionUrl: String,
                           description: String)

  case class SourceResponse(`type`: String,
                            name: String,
                            createdAt: String,
                            updatedAt: String,
                            connectionUrl: String,
                            description: String)

  case class ListSourcesResponse(sources: List[SourceResponse])

  case class DatasetField(`type`: String, name: String, tags: List[String], description: Option[String] = None)
  case class DatasetId(namespace: String, name: String)
  case class DatasetRequest(`type`: String,
                            physicalName: String,
                            sourceName: String,
                            fields: List[DatasetField],
                            description: String)

  case class DatasetResponse(id: DatasetId,
                             `type`: String,
                             name: String,
                             physicalName: String,
                             createdAt: String,
                             updatedAt: String,
                             sourceName: String,
                             namespace: String,
                             fields: List[DatasetField],
                             tags: List[String],
                             lastModifiedAt: Option[String] = None,
                             description: String)

  case class ListDatasetsResponseFormat(datasets: List[DatasetResponse])

  def unmarshalTo[T](response: Future[HttpResponse])
                    (implicit ec: ExecutionContext, mat: Materializer, um: Unmarshaller[ResponseEntity, T]): Future[Either[DefaultErrorResponse, T]] = {
    response
      .flatMap {
        case v if v.status.isSuccess() => Unmarshal(v.entity).to[T].map(r => Right(r).asInstanceOf[Either[DefaultErrorResponse, T]])
        case error => Future.successful(Left(DefaultErrorResponse(error.status.intValue(), error.status.reason())))
      }
  }

}
