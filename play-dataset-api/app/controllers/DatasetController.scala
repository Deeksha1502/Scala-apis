package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import org.apache.pekko.util.Timeout
import org.apache.pekko.actor.{ActorSystem => ClassicActorSystem}
import org.apache.pekko.actor.typed.scaladsl.adapter._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import actors.DatasetManagerActor._
import models.Dataset
import services.DatasetService
import actors.DatasetManagerActor
import actors.DatasetManagerActor._

@Singleton
class DatasetController @Inject()(
                                   cc: ControllerComponents,
                                   datasetService: DatasetService,
                                   classicSystem: ClassicActorSystem
                                 )(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  // convert classic to typed actor system
  private implicit val system = classicSystem.toTyped

  // create actor (typed) once
  private val datasetActor: ActorRef[Command] =
    system.systemActorOf(DatasetManagerActor(datasetService), "dataset-manager-actor")

  implicit val timeout: Timeout = Timeout(5.seconds)

  /** Create a new dataset */
  def createDataset: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[Dataset].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(errors)))),
      dataset => {
        val result: Future[Response] = datasetActor.ask(replyTo => CreateDataset(dataset, replyTo))
        result.map {
          case ActionSuccess(msg) => Ok(Json.obj("message" -> msg))
          case ActionFailure(err) => InternalServerError(Json.obj("error" -> err))
          case _ => InternalServerError(Json.obj("error" -> "Unexpected response"))
        }
      }
    )
  }

  /** Fetch all datasets */
  def getAllDatasets: Action[AnyContent] = Action.async {
    val result: Future[Response] = datasetActor.ask(replyTo => GetAllDatasets(replyTo))
    result.map {
      case AllDatasetsFetched(datasets) => Ok(Json.toJson(datasets))
      case ActionFailure(err)           => InternalServerError(Json.obj("error" -> err))
      case _                            => InternalServerError(Json.obj("error" -> "Unexpected response"))
    }
  }

  /** Fetch a dataset by ID */
  def getDatasetById(id: String): Action[AnyContent] = Action.async {
    val result: Future[Response] = datasetActor.ask(replyTo => GetDataset(id, replyTo))
    result.map {
      case DatasetFetched(Some(dataset)) => Ok(Json.toJson(dataset))
      case DatasetFetched(None)          => NotFound(Json.obj("error" -> s"Dataset with id $id not found"))
      case ActionFailure(err)            => InternalServerError(Json.obj("error" -> err))
      case _                             => InternalServerError(Json.obj("error" -> "Unexpected response"))
    }
  }

  /** Delete a dataset by ID */
  def deleteDataset(id: String): Action[AnyContent] = Action.async {
    val result: Future[Response] = datasetActor.ask(replyTo => DeleteDataset(id, replyTo))
    result.map {
      case ActionSuccess(msg) => Ok(Json.obj("message" -> msg))
      case ActionFailure(err) => InternalServerError(Json.obj("error" -> err))
      case _ => InternalServerError(Json.obj("error" -> "Unexpected response"))
    }
  }
}
