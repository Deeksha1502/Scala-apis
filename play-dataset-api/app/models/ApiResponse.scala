package models
import play.api.libs.json._
import java.time.Instant
import java.util.UUID

case class ResponseParams(
    msgid: String,
    status: String,
    err: Option[String],
    errmsg: Option[String]
)

object ResponseParams {
    implicit val format: OFormat[ResponseParams] = Json.format[ResponseParams]

    def success(): ResponseParams = ResponseParams(
        msgid = UUID.randomUUID().toString,
        status = "success",
        err = None,
        errmsg = None
    )
    
    def failure(errCode: String, errMsg: String): ResponseParams = ResponseParams(
        msgid = UUID.randomUUID().toString,
        status = "failed",  // Changed from "failure" to "failed"
        err = Some(errCode),
        errmsg = Some(errMsg)
    )
}

case class ApiResponse(  // Changed { to (
    id: String,
    ver: String,
    ts: String,
    params: ResponseParams,
    responseCode: String,
    result: JsObject
)  // Changed } to )

object ApiResponse {
    implicit val format: OFormat[ApiResponse] = Json.format[ApiResponse]
    
    def success(apiId: String, result: JsObject): ApiResponse = ApiResponse(
        id = apiId,
        ver = "v1",
        ts = Instant.now().toString,
        params = ResponseParams.success(),
        responseCode = "OK",
        result = result
    )
    
    def error(apiId: String, statusCode: String, errCode: String, errMsg: String): ApiResponse = ApiResponse(
        id = apiId,
        ver = "v1",
        ts = Instant.now().toString,
        params = ResponseParams.failure(errCode, errMsg),
        responseCode = statusCode,
        result = Json.obj()
    )
}