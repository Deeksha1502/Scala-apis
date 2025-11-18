package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import db.CassandraConnector
import models.ApiResponse

@Singleton
class HealthController @Inject()(cc: ControllerComponents, connector: CassandraConnector) extends AbstractController(cc) {

  def dbHealth: Action[AnyContent] = Action {
    try {
      val session = connector.getSession
      val clusterName = session.getMetadata.getClusterName
      
      val response = ApiResponse.success(
        apiId = "api.health.check",
        result = Json.obj("message" -> s"Connected to Cassandra cluster: $clusterName")
      )
      
      Ok(Json.toJson(response))
    } catch {
      case ex: Exception =>
        val response = ApiResponse.error(
          apiId = "api.health.check",
          statusCode = "INTERNAL_SERVER_ERROR",
          errCode = "DB_CONNECTION_ERROR",
          errMsg = ex.getMessage
        )
        
        InternalServerError(Json.toJson(response))
    }
  }
}