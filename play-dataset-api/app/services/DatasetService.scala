package services

import javax.inject._
import db.CassandraConnector
import models.Dataset
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

@Singleton
class DatasetService @Inject()(connector: CassandraConnector)(implicit ec: ExecutionContext) {

  private val session = connector.getSession

  def createDataset(dataset: Dataset): Future[String] = Future {
    val query =
      s"""
         |INSERT INTO datasets (id, dataset_schema, router_config, status,
         | created_by, updated_by, created_date, updated_date)
         |VALUES ('${dataset.id}', '${dataset.dataset_schema}', '${dataset.router_config}', '${dataset.status}',
         |'${dataset.created_by}', '${dataset.updated_by}', toTimestamp(now()), toTimestamp(now()));
         |""".stripMargin
    session.execute(query)
    s"Dataset ${dataset.id} inserted successfully"
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

}
