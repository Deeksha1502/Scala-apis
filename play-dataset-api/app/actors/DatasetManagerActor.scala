package actors

import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import services.DatasetService
import models.Dataset
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object DatasetManagerActor {

  // ----- Messages -----
  sealed trait Command

  case class CreateDataset(dataset: Dataset, replyTo: ActorRef[Response]) extends Command
  case class GetDataset(id: String, replyTo: ActorRef[Response]) extends Command
  case class DeleteDataset(id: String, replyTo: ActorRef[Response]) extends Command
  case class GetAllDatasets(replyTo: ActorRef[Response]) extends Command


  // ----- Responses -----
  sealed trait Response
  case class ActionSuccess(message: String) extends Response
  case class ActionFailure(reason: String) extends Response
  case class DatasetFetched(dataset: Option[Dataset]) extends Response
  case class AllDatasetsFetched(datasets: Seq[Dataset]) extends Response


  // ----- Actor Behavior -----
  def apply(datasetService: DatasetService)(implicit ec: ExecutionContext): Behavior[Command] = {
    Behaviors.receiveMessage {
      case CreateDataset(dataset, replyTo) =>
        datasetService.createDataset(dataset).onComplete {
          case Success(msg) => replyTo ! ActionSuccess(msg)
          case Failure(ex)  => replyTo ! ActionFailure(ex.getMessage)
        }
        Behaviors.same


      case GetAllDatasets(replyTo) =>
        datasetService.getAllDatasets().onComplete {
          case Success(datasets) => replyTo ! AllDatasetsFetched(datasets)
          case Failure(ex)       => replyTo ! ActionFailure(ex.getMessage)
        }
        Behaviors.same


      case GetDataset(id, replyTo) =>
        datasetService.getDatasetById(id).onComplete {
          case Success(opt) => replyTo ! DatasetFetched(opt)
          case Failure(ex)  => replyTo ! ActionFailure(ex.getMessage)
        }
        Behaviors.same

      case DeleteDataset(id, replyTo) =>
        datasetService.deleteDatasetById(id).onComplete {
          case Success(msg) => replyTo ! ActionSuccess(msg)
          case Failure(ex)  => replyTo ! ActionFailure(ex.getMessage)
        }
        Behaviors.same
    }
  }
}
