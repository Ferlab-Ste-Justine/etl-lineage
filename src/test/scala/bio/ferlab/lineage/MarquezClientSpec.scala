package bio.ferlab.lineage

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import bio.ferlab.lineage.MarquezClient.{DatasetField, DatasetId, DatasetRequest, JobRequest, NamespaceRequest, SourceRequest}
import org.scalatest.GivenWhenThen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class MarquezClientSpec extends AnyFlatSpec with GivenWhenThen with WithSparkSession with Matchers {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SingleRequest")
  implicit val executionContext: ExecutionContext = system.executionContext
  Given("Marquez App running locally")
  val marquez = new MarquezClient()

  "MarquezClient" should "create a namespace" in {
    val resp = marquez.createNamespace("my-namespace", NamespaceRequest("me", "mine"))
    val result = Await.result(resp, Duration.Inf)
    println(result)
    result.isRight shouldBe true

    val resp2 = marquez.getNamespace("my-namespace")
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

  "MarquezClient" should "create a source" in {
    val resp = marquez.createSource("my-source", SourceRequest("POSTGRESQL", "jdbc:postgresql://localhost:5431/mydb", "src"))
    val result = Await.result(resp, Duration.Inf)
    println(result)
    result.isRight shouldBe true
    And("a source is created")

    val resp2 = marquez.getSource("my-source")
    val result2 = Await.result(resp2, Duration.Inf)
    println(result2)
    result2.isRight shouldBe true
    result2.right shouldBe result.right

    val resp3 = marquez.getSources()
    val result3 = Await.result(resp3, Duration.Inf)
    println(result3)
    result3.isRight shouldBe true
    result3.right.get.sources should contain oneElementOf Seq(result.right.get)
  }

  "MarquezClient" should "create a dataset" in {
    val resp = marquez.createDataset(
      "my-namespace",
      "my-dataset",
      DatasetRequest("DB_TABLE", "public.mytable", "my-source", List(DatasetField("INTEGER", "a", List(), Some("desc"))), "desc2"))
    val result = Await.result(resp, Duration.Inf)
    println(result)
    result.isRight shouldBe true

    val resp2 = marquez.getDataset("my-namespace", "my-dataset")
    val result2 = Await.result(resp2, Duration.Inf)
    println(result2)
    result2.isRight shouldBe true
    result2.right shouldBe result.right

    val resp3 = marquez.getDatasets("my-namespace")
    val result3 = Await.result(resp3, Duration.Inf)
    println(result3)
    result3.isRight shouldBe true
    result3.right.get.datasets should contain oneElementOf Seq(result.right.get)
  }

  "MarquezClient" should "create a job" in {
    val resp = marquez.createJob(
      "my-namespace",
      "my-job",
      JobRequest("BATCH", List(DatasetId("my-namespace", "my-dataset")), List(DatasetId("my-namespace", "my-dataset")), "https://github.com/somewhere", "desc2"))
    val result = Await.result(resp, Duration.Inf)
    println(result)
    result.isRight shouldBe true

    val resp2 = marquez.getJob("my-namespace", "my-job")
    val result2 = Await.result(resp2, Duration.Inf)
    println(result2)
    result2.isRight shouldBe true
    result2.right shouldBe result.right

    val resp3 = marquez.getJobs("my-namespace")
    val result3 = Await.result(resp3, Duration.Inf)
    println(result3)
    result3.isRight shouldBe true
    result3.right.get.jobs should contain oneElementOf Seq(result.right.get)
  }
}
