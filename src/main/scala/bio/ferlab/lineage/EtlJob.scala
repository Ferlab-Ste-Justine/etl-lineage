package bio.ferlab.lineage

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import bio.ferlab.lineage.MarquezService._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

object EtlJob extends App with Spark with MarquezMonitoring {

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SingleRequest")
  implicit val executionContext: ExecutionContext = system.executionContext

  implicit val marquez: MarquezService = new MarquezService()

  val namespace = "my-namespace"
  val source = "my-postgres"
  val destination = "my-s3"
  val sourceDataset = "source-dataset"
  val destinationDataset = "destination-dataset"
  val job = "job-source-to-destination"

  val namespaceRequest = NamespaceRequest("me", "mine")
  val sourceRequest = SourceRequest("POSTGRESQL", "jdbc:postgresql://localhost:5431/mydb", "source")
  val destinationRequest = SourceRequest("POSTGRESQL", "s3://mybucket/destination", "destination")
  val datasetSourceRequest = DatasetRequest("DB_TABLE", "public.source", source, List(DatasetField("STRING", "a", List("SQL"), Some("field a")), DatasetField("INTEGER", "b", List("SQL"), Some("field b"))), "source table")
  val datasetDestinationRequest = DatasetRequest("DB_TABLE", "public.destination", destination, List(DatasetField("INTEGER", "aaa", List("s3"), Some("field aaa"))), "destination table")
  val jobRequest = JobRequest("BATCH", List(DatasetId(namespace, sourceDataset)), List(DatasetId(namespace, destinationDataset)), "https://github.com/somewhere", "desc2")
  val runArguments = RunArguments("cbotek@ferlab.bio", "true", "true", "1")

  //setup marquez namespace, source, dataset and job
  val futureResult = marquez.createNamespace(namespace, namespaceRequest)
    .flatMap(_=> marquez.createSource(source, sourceRequest))
    .flatMap(_=> marquez.createSource(destination, destinationRequest))
    .flatMap(_=> marquez.createDataset(namespace, source, datasetSourceRequest))
    .flatMap(_=> marquez.createDataset(namespace, destination, datasetDestinationRequest))
    .flatMap(_=> marquez.createJob(namespace, job, jobRequest))
    .flatMap(_.fold(e => Future.successful(Left(e)),
      jobResponse => {

        //run the actual job
        withMarquezMonitoring(namespace, job, runArguments){runId =>
          if (Random.nextBoolean()) throw new Exception(s"job [$job:$runId] FAILED")
          else s"job [$job:$runId] SUCCEEDED"
        }
      }))
    .foreach { result =>

    //let's check the result
    println(result)

    //Do not forget to shutdown Akka or the process won't finish
    system.terminate()
  }

}
