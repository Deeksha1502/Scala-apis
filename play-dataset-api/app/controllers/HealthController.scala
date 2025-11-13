package controllers

import javax.inject._
import play.api.mvc._
import db.CassandraConnector

@Singleton
class HealthController @Inject()(cc: ControllerComponents, connector: CassandraConnector) extends AbstractController(cc) {

  def dbHealth: Action[AnyContent] = Action {
    try {
      val session = connector.getSession
      val clusterName = session.getMetadata.getClusterName
      Ok(s"Connected to Cassandra cluster: $clusterName")
    } catch {
      case ex: Exception =>
        InternalServerError(s"Cassandra connection failed: ${ex.getMessage}")
    }
  }
}
