package utils

import javax.inject._
import redis.RedisClient
import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem
import play.api.Configuration

@Singleton
class RedisConnector @Inject()(config: Configuration, actorSystem: ActorSystem)(implicit ec: ExecutionContext) {
  
  private val host = config.getOptional[String]("redis.host").getOrElse("127.0.0.1")
  private val port = config.getOptional[Int]("redis.port").getOrElse(6379)
  
  // Use injected ActorSystem
  implicit val system: ActorSystem = actorSystem
  
  val client: RedisClient = RedisClient(host = host, port = port)
  
  // Close Redis connection on shutdown
  sys.addShutdownHook {
    client.quit()
  }
}
