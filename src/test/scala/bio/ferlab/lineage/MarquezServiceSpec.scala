package bio.ferlab.lineage

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import bio.ferlab.lineage.MarquezService._
import org.scalatest.GivenWhenThen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class MarquezServiceSpec extends AnyFlatSpec with GivenWhenThen with TestSparkSession with Matchers {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SingleRequest")
  implicit val executionContext: ExecutionContext = system.executionContext

  val marquez = new MarquezService()
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
  val runRequest = RunRequest(args = RunArguments("cbotek@ferlab.bio", "true", "true", "1"))

  "MarquezClient" should "create a namespace" in {
    val resp = marquez.createNamespace(namespace, namespaceRequest)
    val result = Await.result(resp, Duration.Inf)
    println(result)
    result.isRight shouldBe true

    val resp2 = marquez.getNamespace(namespace)
    val result2 = Await.result(resp2, Duration.Inf)
    println(result2)
    result2.isRight shouldBe true
    result2.right shouldBe result.right

    val resp3 = marquez.getNamespaces()
    val result3 = Await.result(resp3, Duration.Inf)
    println(result3)
    result3.isRight shouldBe true
    result3.right.get.namespaces should contain oneElementOf Seq(result.right.get)
  }

  "MarquezClient" should "create two sources" in {
    val resp = marquez.createSource(source, sourceRequest)
    val result = Await.result(resp, Duration.Inf)
    println(result)
    result.isRight shouldBe true

    val resp2 = marquez.getSource(source)
    val result2 = Await.result(resp2, Duration.Inf)
    println(result2)
    result2.isRight shouldBe true
    result2.right shouldBe result.right

    val resp3 = marquez.getSources()
    val result3 = Await.result(resp3, Duration.Inf)
    println(result3)
    result3.isRight shouldBe true
    result3.right.get.sources should contain oneElementOf Seq(result.right.get)

    val resp4 = marquez.createSource(destination, destinationRequest)
    val result4 = Await.result(resp4, Duration.Inf)
    println(result4)
    result4.isRight shouldBe true
  }

  "MarquezClient" should "create two datasets" in {
    val resp = marquez.createDataset(namespace, sourceDataset, datasetSourceRequest)
    val result = Await.result(resp, Duration.Inf)
    println(result)
    result.isRight shouldBe true

    val resp2 = marquez.getDataset(namespace, sourceDataset)
    val result2 = Await.result(resp2, Duration.Inf)
    println(result2)
    result2.isRight shouldBe true
    result2.right shouldBe result.right

    val resp3 = marquez.getDatasets(namespace)
    val result3 = Await.result(resp3, Duration.Inf)
    println(result3)
    result3.isRight shouldBe true
    result3.right.get.datasets should contain oneElementOf Seq(result.right.get)

    val resp4 = marquez.createDataset(namespace, destinationDataset, datasetDestinationRequest)
    val result4 = Await.result(resp4, Duration.Inf)
    println(result4)
    result4.isRight shouldBe true
  }

  "MarquezClient" should "create a job" in {
    val resp = marquez.createJob(namespace, job, jobRequest)
    val result = Await.result(resp, Duration.Inf)
    println(result)
    result.isRight shouldBe true

    val resp2 = marquez.getJob(namespace, job)
    val result2 = Await.result(resp2, Duration.Inf)
    println(result2)
    result2.isRight shouldBe true
    result2.right shouldBe result.right

    val resp3 = marquez.getJobs(namespace)
    val result3 = Await.result(resp3, Duration.Inf)
    println(result3)
    result3.isRight shouldBe true
    result3.right.get.jobs should contain oneElementOf Seq(result.right.get)
  }

  "MarquezClient" should "create a run" in {
    val resp = marquez.createRun(namespace, job, runRequest)
    val run = Await.result(resp, Duration.Inf)
    println(run)
    run.isRight shouldBe true
    run.right.get.state shouldBe "NEW"
    val runId = run.right.get.id

    val resp2 = marquez.startRun(runId)
    val result2 = Await.result(resp2, Duration.Inf)
    println(result2)
    result2.isRight shouldBe true
    result2.right.get.state shouldBe "RUNNING"

    val resp3 = marquez.completeRun(runId)
    val result3 = Await.result(resp3, Duration.Inf)
    println(result3)
    result3.isRight shouldBe true
    result3.right.get.state shouldBe "COMPLETED"

    val resp4 = marquez.failRun(runId)
    val result4 = Await.result(resp4, Duration.Inf)
    println(result4)
    result4.isRight shouldBe true
    result4.right.get.state shouldBe "FAILED"
  }
}
