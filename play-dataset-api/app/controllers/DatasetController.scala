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
import models.{Dataset, ApiResponse}  // Add ApiResponse import
import services.DatasetService
import actors.DatasetManagerActor

@Singleton
class DatasetController @Inject()(
  cc: ControllerComponents,
  datasetService: DatasetService,
  classicSystem: ClassicActorSystem
)(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  private implicit val system = classicSystem.toTyped
  private val datasetActor: ActorRef[Command] =
    system.systemActorOf(DatasetManagerActor(datasetService), "dataset-manager-actor")

  implicit val timeout: Timeout = Timeout(5.seconds)

  /** Create a new dataset */
  def createDataset: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[Dataset].fold(
      errors => {
        val response = ApiResponse.error(
          apiId = "api.dataset.create",
          statusCode = "BAD_REQUEST",
          errCode = "VALIDATION_ERROR",
          errMsg = JsError.toJson(errors).toString()
        )
        Future.successful(BadRequest(Json.toJson(response)))
      },
      dataset => {
        val result: Future[Response] = datasetActor.ask(replyTo => CreateDataset(dataset, replyTo))
        result.map {
          case ActionSuccess(msg) =>
            val response = ApiResponse.success(
              apiId = "api.dataset.create",
              result = Json.obj("message" -> msg)
            )
            Ok(Json.toJson(response))
            
          case ActionFailure(err) =>
            val response = ApiResponse.error(
              apiId = "api.dataset.create",
              statusCode = "INTERNAL_SERVER_ERROR",
              errCode = "DB_ERROR",
              errMsg = err
            )
            InternalServerError(Json.toJson(response))
            
          case _ =>
            val response = ApiResponse.error(
              apiId = "api.dataset.create",
              statusCode = "INTERNAL_SERVER_ERROR",
              errCode = "UNEXPECTED_ERROR",
              errMsg = "Unexpected response from actor"
            )
            InternalServerError(Json.toJson(response))
        }
      }
    )
  }

  /** Fetch all datasets */
  def getAllDatasets: Action[AnyContent] = Action.async {
    val result: Future[Response] = datasetActor.ask(replyTo => GetAllDatasets(replyTo))
    result.map {
      case AllDatasetsFetched(datasets) =>
        val response = ApiResponse.success(
          apiId = "api.dataset.read",
          result = Json.obj("datasets" -> Json.toJson(datasets))
        )
        Ok(Json.toJson(response))
        
      case ActionFailure(err) =>
        val response = ApiResponse.error(
          apiId = "api.dataset.read",
          statusCode = "INTERNAL_SERVER_ERROR",
          errCode = "DB_ERROR",
          errMsg = err
        )
        InternalServerError(Json.toJson(response))
        
      case _ =>
        val response = ApiResponse.error(
          apiId = "api.dataset.read",
          statusCode = "INTERNAL_SERVER_ERROR",
          errCode = "UNEXPECTED_ERROR",
          errMsg = "Unexpected response from actor"
        )
        InternalServerError(Json.toJson(response))
    }
  }

  /** Fetch a dataset by ID */
  def getDatasetById(id: String): Action[AnyContent] = Action.async {
    val result: Future[Response] = datasetActor.ask(replyTo => GetDataset(id, replyTo))
    result.map {
      case DatasetFetched(Some(dataset)) =>
        val response = ApiResponse.success(
          apiId = "api.dataset.read",
          result = Json.obj("dataset" -> Json.toJson(dataset))
        )
        Ok(Json.toJson(response))
        
      case DatasetFetched(None) =>
        val response = ApiResponse.error(
          apiId = "api.dataset.read",
          statusCode = "NOT_FOUND",
          errCode = "DATASET_NOT_FOUND",
          errMsg = s"Dataset with id $id not found"
        )
        NotFound(Json.toJson(response))
        
      case ActionFailure(err) =>
        val response = ApiResponse.error(
          apiId = "api.dataset.read",
          statusCode = "INTERNAL_SERVER_ERROR",
          errCode = "DB_ERROR",
          errMsg = err
        )
        InternalServerError(Json.toJson(response))
        
      case _ =>
        val response = ApiResponse.error(
          apiId = "api.dataset.read",
          statusCode = "INTERNAL_SERVER_ERROR",
          errCode = "UNEXPECTED_ERROR",
          errMsg = "Unexpected response from actor"
        )
        InternalServerError(Json.toJson(response))
    }
  }

  /** Delete a dataset by ID */
  def deleteDataset(id: String): Action[AnyContent] = Action.async {
    val result: Future[Response] = datasetActor.ask(replyTo => DeleteDataset(id, replyTo))
    result.map {
      case ActionSuccess(msg) =>
        val response = ApiResponse.success(
          apiId = "api.dataset.delete",
          result = Json.obj("message" -> msg)
        )
        Ok(Json.toJson(response))
        
      case ActionFailure(err) =>
        val response = ApiResponse.error(
          apiId = "api.dataset.delete",
          statusCode = "INTERNAL_SERVER_ERROR",
          errCode = "DB_ERROR",
          errMsg = err
        )
        InternalServerError(Json.toJson(response))
        
      case _ =>
        val response = ApiResponse.error(
          apiId = "api.dataset.delete",
          statusCode = "INTERNAL_SERVER_ERROR",
          errCode = "UNEXPECTED_ERROR",
          errMsg = "Unexpected response from actor"
        )
        InternalServerError(Json.toJson(response))
    }
  }
}