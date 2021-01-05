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
  val source = "my-source"
  val dataset = "my-dataset"
  val job = "my-job"

  val namespaceRequest = NamespaceRequest("me", "mine")
  val sourceRequest = SourceRequest("POSTGRESQL", "jdbc:postgresql://localhost:5431/mydb", "src")
  val datasetRequest = DatasetRequest("DB_TABLE", "public.mytable", source, List(DatasetField("INTEGER", "a", List(), Some("desc"))), "desc2")
  val jobRequest = JobRequest("BATCH", List(DatasetId(namespace, dataset)), List(DatasetId(namespace, dataset)), "https://github.com/somewhere", "desc2")
  val runArguments = RunArguments("cbotek@ferlab.bio", "true", "true", "1")

  //setup marquez namespace, source, dataset and job
  val futureResult = marquez.createNamespace(namespace, namespaceRequest)
    .flatMap(_.fold(e => Future.successful(Left(e)), _ => marquez.createSource(source, sourceRequest)))
    .flatMap(_.fold(e => Future.successful(Left(e)), _ => marquez.createDataset(namespace, dataset, datasetRequest)))
    .flatMap(_.fold(e => Future.successful(Left(e)), _ => marquez.createJob(namespace, job, jobRequest)))
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
