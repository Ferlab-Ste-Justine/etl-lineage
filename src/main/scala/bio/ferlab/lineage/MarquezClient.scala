package bio.ferlab.lineage

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
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
  implicit val listDatasetsResponseFormat = jsonFormat1(ListDatasetsResponse)

  implicit val jobRequestFormat = jsonFormat5(JobRequest)
  implicit val jobResponseFormat = jsonFormat12(JobResponse)
  implicit val listJobsResponseFormat = jsonFormat1(ListJobsResponse)

  implicit val runArgumentsFormat = jsonFormat4(RunArguments)
  implicit val runRequestFormat = jsonFormat1(RunRequest)
  implicit val runResponseFormat = jsonFormat10(RunResponse)


  val apiV1Url = s"$baseUrl/api/v1"
  val namespacesUrl = s"$apiV1Url/namespaces"
  val sourcesUrl = s"$apiV1Url/sources"
  val tagsUrl = s"$apiV1Url/tags"
  val runsUrl = s"$apiV1Url/jobs/runs"

  def createDataset(namespace: String, name: String, body: DatasetRequest)
                   (implicit ec: ExecutionContext, system: ActorSystem[Nothing]):
  Future[Either[DefaultErrorResponse, DatasetResponse]] =
    HttpClient.PUT[DatasetRequest, DatasetResponse](s"$namespacesUrl/$namespace/datasets/$name", body)

  def getDataset(namespace: String, name: String)
                (implicit ec: ExecutionContext, system: ActorSystem[Nothing]): Future[Either[DefaultErrorResponse, DatasetResponse]] =
    HttpClient.GET[DatasetResponse](s"$namespacesUrl/$namespace/datasets/$name")

  def getDatasets(namespace: String)(implicit ec: ExecutionContext, system: ActorSystem[Nothing]):
  Future[Either[DefaultErrorResponse, ListDatasetsResponse]] =
    HttpClient.GET[ListDatasetsResponse](s"$namespacesUrl/$namespace/datasets")

  def createJob(namespace: String, name: String, body: JobRequest)
               (implicit ec: ExecutionContext, system: ActorSystem[Nothing]):
  Future[Either[DefaultErrorResponse, JobResponse]] =
    HttpClient.PUT[JobRequest, JobResponse](s"$namespacesUrl/$namespace/jobs/$name", body)

  def getJob(namespace: String, name: String)
            (implicit ec: ExecutionContext, system: ActorSystem[Nothing]): Future[Either[DefaultErrorResponse, JobResponse]] =
    HttpClient.GET[JobResponse](s"$namespacesUrl/$namespace/jobs/$name")

  def getJobs(namespace: String)(implicit ec: ExecutionContext, system: ActorSystem[Nothing]):
  Future[Either[DefaultErrorResponse, ListJobsResponse]] =
    HttpClient.GET[ListJobsResponse](s"$namespacesUrl/$namespace/jobs")

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

  def createRun(namespace: String, job: String, body: RunRequest)
                  (implicit ec: ExecutionContext, system: ActorSystem[Nothing]):
  Future[Either[DefaultErrorResponse, RunResponse]] =
    HttpClient.POST[RunRequest, RunResponse](s"$namespacesUrl/$namespace/jobs/$job/runs", body)

  def startRun(runId: String)
              (implicit ec: ExecutionContext, system: ActorSystem[Nothing]):
  Future[Either[DefaultErrorResponse, RunResponse]] =
    HttpClient.POST[String, RunResponse](s"$runsUrl/$runId/start")

  def completeRun(runId: String)
                 (implicit ec: ExecutionContext, system: ActorSystem[Nothing]):
  Future[Either[DefaultErrorResponse, RunResponse]] =
    HttpClient.POST[String, RunResponse](s"$runsUrl/$runId/complete")

  def failRun(runId: String)
             (implicit ec: ExecutionContext, system: ActorSystem[Nothing]):
  Future[Either[DefaultErrorResponse, RunResponse]] =
    HttpClient.POST[String, RunResponse](s"$runsUrl/$runId/fail")
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

  case class DatasetField(`type`: String,
                          name: String,
                          tags: List[String],
                          description: Option[String] = None)

  case class DatasetId(namespace: String,
                       name: String)

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

  case class ListDatasetsResponse(datasets: List[DatasetResponse])

  case class JobRequest(`type`: String,
                        inputs: List[DatasetId],
                        outputs: List[DatasetId],
                        location: String,
                        description: String)

  case class JobResponse(id: DatasetId,
                         `type`: String,
                         name: String,
                         createdAt: String,
                         updatedAt: String,
                         namespace: String,
                         inputs: List[DatasetId],
                         outputs: List[DatasetId],
                         location: String,
                         context: Map[String, String],
                         description: String,
                         latestRun: Option[String] = None)

  case class ListJobsResponse(jobs: List[JobResponse])

  case class RunArguments(email: String,
                          emailOnFailure: String,
                          emailOnRetry: String,
                          retries: String)

  case class RunRequest(args: RunArguments)

  case class RunResponse(id: String,
                         createdAt: String,
                         updatedAt: String,
                         nominalStartTime: Option[String] = None,
                         nominalEndTime: Option[String] = None,
                         state: String,
                         startedAt: Option[String] = None,
                         endedAt: Option[String] = None,
                         durationMs: Option[Long] = None,
                         args: RunArguments)

}
