package models

import play.api.libs.json._
import java.time.Instant

case class Dataset(
                    id: String,
                    dataset_schema: String,
                    router_config: String,
                    status: String,
                    created_by: String,
                    updated_by: String,
                    created_date: Instant,
                    updated_date: Instant
                  )

object Dataset {
  implicit val datasetFormat: OFormat[Dataset] = Json.format[Dataset]
}
