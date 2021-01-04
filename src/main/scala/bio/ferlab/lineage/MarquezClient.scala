package bio.ferlab.lineage

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.Materializer
import bio.ferlab.lineage.HttpClient.DefaultErrorResponse
import bio.ferlab.lineage.MarquezClient._
import spray.json.DefaultJsonProtocol

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
  Future[Either[DefaultErrorResponse, DatasetResponse]] =
    HttpClient.PUT[DatasetRequest, DatasetResponse](s"$namespacesUrl/$namespace/datasets/$name", body)

  def getDataset(namespace: String, name: String)
                (implicit ec: ExecutionContext, system: ActorSystem[Nothing]): Future[Either[DefaultErrorResponse, DatasetResponse]] =
    HttpClient.GET[DatasetResponse](s"$namespacesUrl/$namespace/datasets/$name")

  def getDatasets(namespace: String)(implicit ec: ExecutionContext, system: ActorSystem[Nothing]):
  Future[Either[DefaultErrorResponse, ListDatasetsResponseFormat]] =
    HttpClient.GET[ListDatasetsResponseFormat](s"$namespacesUrl/$namespace/datasets")

  def createNamespace(name: String, body: NamespaceRequest)
                     (implicit ec: ExecutionContext, system: ActorSystem[Nothing]):
  Future[Either[DefaultErrorResponse, NamespaceResponse]] =
    HttpClient.PUT[NamespaceRequest, NamespaceResponse](s"$namespacesUrl/$name", body)

  def getNamespace(name: String)
                  (implicit ec: ExecutionContext, system: ActorSystem[Nothing]):
  Future[Either[DefaultErrorResponse, NamespaceResponse]] =
    HttpClient.GET[NamespaceResponse](s"$namespacesUrl/$name")

  def getNamespaces()
                   (implicit ec: ExecutionContext, system: ActorSystem[Nothing]):
  Future[Either[DefaultErrorResponse, ListNamespacesResponse]] =
    HttpClient.GET[ListNamespacesResponse](s"$namespacesUrl/")

  def createSource(name: String, body: SourceRequest)
                  (implicit ec: ExecutionContext, system: ActorSystem[Nothing]):
  Future[Either[DefaultErrorResponse, SourceResponse]] =
    HttpClient.PUT[SourceRequest, SourceResponse](s"$sourcesUrl/$name", body)

  def getSource(name: String)
               (implicit ec: ExecutionContext, system: ActorSystem[Nothing]):
  Future[Either[DefaultErrorResponse, SourceResponse]] =
    HttpClient.GET[SourceResponse](s"$sourcesUrl/$name")

  def getSources()
                (implicit ec: ExecutionContext, system: ActorSystem[Nothing]):
  Future[Either[DefaultErrorResponse, ListSourcesResponse]] =
    HttpClient.GET[ListSourcesResponse](s"$sourcesUrl/")
}

object MarquezClient {

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
