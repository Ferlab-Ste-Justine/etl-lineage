package bio.ferlab.lineage

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import bio.ferlab.lineage.MarquezClient.NamespaceRequest
import org.apache.spark.sql.SparkSession

object EtlJob extends App {

  implicit val spark: SparkSession =
    SparkSession.builder()
      .master("local")
      .getOrCreate()

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SingleRequest")
  implicit val executionContext = system.executionContext

  //create run



  //start job


  new MarquezClient()
    .createNamespace("my-namespace", NamespaceRequest("me", "description"))
    .map {_.fold(e =>
      //Failed job
      println(e.toString)
      //println("Job failed")
      ,v=>
        //Completed job
        println(v.toString)
        //println("Job Completed")
    )
    }


}
