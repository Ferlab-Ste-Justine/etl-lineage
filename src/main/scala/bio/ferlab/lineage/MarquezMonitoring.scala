package bio.ferlab.lineage

import akka.actor.typed.ActorSystem
import bio.ferlab.lineage.MarquezService.{RunArguments, RunRequest}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait MarquezMonitoring {

  def withMarquezMonitoring[T](namespace: String, jobName: String, args: RunArguments)(job: String => T)
                              (implicit marquez: MarquezService, ec: ExecutionContext, system: ActorSystem[Nothing]):
  Future[Either[Throwable, T]] = {
    marquez
      .createRun(namespace, jobName, RunRequest(args))
      .map(_.fold(
        e => {
          Left(new Exception(s"Error statusCode: ${e.statusCode} with reason: ${e.message}"))
        },
        run => {
          Try(job(run.id))
            .fold[Either[Throwable, T]]( t => {
              marquez.failRun(run.id)
              Left(t)
            }, result => {
              marquez.completeRun(run.id)
              Right(result)
            }
            )
        }
      ))
  }

}
