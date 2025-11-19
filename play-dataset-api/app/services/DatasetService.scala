package services

import javax.inject._
import db.CassandraConnector
import utils.RedisConnector
import models.Dataset
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import play.api.libs.json._

@Singleton
class DatasetService @Inject()(
  connector: CassandraConnector,
  redisConnector: RedisConnector
)(implicit ec: ExecutionContext) {

  private val client = redisConnector.client

  private val session = connector.getSession

  def createDataset(dataset: Dataset): Future[String] = {
    val query =
      s"""
         |INSERT INTO datasets (id, dataset_schema, router_config, status,
         | created_by, updated_by, created_date, updated_date)
         |VALUES ('${dataset.id}', '${dataset.dataset_schema}', '${dataset.router_config}', '${dataset.status}',
         |'${dataset.created_by}', '${dataset.updated_by}', toTimestamp(now()), toTimestamp(now()));
         |""".stripMargin
    
    Future {
      session.execute(query)
      cacheOnCreate(dataset) // Cache the created dataset
      s"Dataset ${dataset.id} inserted successfully"
    }
  }



  def getDatasetById(id: String): Future[Option[Dataset]] = Future {
    val query = s"SELECT * FROM datasets WHERE id = '$id';"
    val rs = session.execute(query)
    val row = rs.one()
    if (row == null) None
    else Some(
      Dataset(
        id = row.getString("id"),
        dataset_schema = row.getString("dataset_schema"),
        router_config = row.getString("router_config"),
        status = row.getString("status"),
        created_by = row.getString("created_by"),
        updated_by = row.getString("updated_by"),
        created_date = row.getInstant("created_date"),
        updated_date = row.getInstant("updated_date")
      )
    )
  }

  def getAllDatasets(): Future[List[Dataset]] = Future {
    val rs = session.execute("SELECT * FROM datasets;")
    rs.all().asScala.toList.map { row =>
      Dataset(
        id = row.getString("id"),
        dataset_schema = row.getString("dataset_schema"),
        router_config = row.getString("router_config"),
        status = row.getString("status"),
        created_by = row.getString("created_by"),
        updated_by = row.getString("updated_by"),
        created_date = row.getInstant("created_date"),
        updated_date = row.getInstant("updated_date")
      )
    }
  }

  def deleteDatasetById(id: String): Future[String] = Future {
    val query = s"DELETE FROM datasets WHERE id = '$id';"
    val result = session.execute(query)
    if (result.wasApplied()) s"Dataset with id '$id' deleted successfully"
    else s"No dataset found with id '$id'"
  }


  def getDatasetCached(id: String): Future[Option[Dataset]] = {
    val key = s"dataset:$id"

    client.get[String](key).flatMap {
      case Some(jsonString) =>
        Future.successful(Some(Json.parse(jsonString).as[Dataset]))
      case None =>
        getDatasetById(id).map {
          case Some(ds) =>
            client.setex(key, 300, Json.toJson(ds).toString())
            Some(ds)
          case None => None
        }
    }
  }

  def getAllDatasetsCached(): Future[List[Dataset]] = {
    val key = "datasets:all"

    client.get[String](key).flatMap {
      case Some(jsonString) =>
        Future.successful(Json.parse(jsonString).as[List[Dataset]])
      case None =>
        getAllDatasets().map { list =>
          client.setex(key, 60, Json.toJson(list).toString())
          list
        }
    }
  }

  def cacheOnCreate(dataset: Dataset): Unit = {
    val key = s"dataset:${dataset.id}"
    client.set(key, Json.toJson(dataset).toString())
    client.del("datasets:all") // clean stale cache for GET ALL
  }

  def removeFromCache(id: String): Unit = {
    client.del(s"dataset:$id")
    client.del("datasets:all")
  }

}
