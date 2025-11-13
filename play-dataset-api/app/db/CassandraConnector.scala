package db

import javax.inject._
import play.api.{Configuration, Logging}
import com.datastax.oss.driver.api.core.CqlSession
import java.net.InetSocketAddress

/**
 * CassandraConnector is responsible for:
 * - Reading Cassandra configuration from application.conf
 * - Creating and managing a single CqlSession
 * - Providing that session to other classes (services/controllers)
 */
@Singleton
class CassandraConnector @Inject()(config: Configuration) extends Logging {

  // Read Cassandra settings from application.conf
  private val contactPointStr = config.getOptional[Seq[String]]("cassandra.contact-points")
    .getOrElse(Seq("127.0.0.1:9042"))
  private val datacenter = config.get[String]("cassandra.datacenter")
  private val keyspace = config.get[String]("cassandra.keyspace")

  // Split host and port from the first contact point
  private val host = contactPointStr.head.split(":")(0)
  private val port = contactPointStr.head.split(":")(1).toInt

  // Initialize a single Cassandra session
  private val session: CqlSession = {
    logger.info(s"Connecting to Cassandra at $host:$port (DC: $datacenter, keyspace: $keyspace)...")
    val s = CqlSession.builder()
      .addContactPoint(new InetSocketAddress(host, port))
      .withLocalDatacenter(datacenter)
      .withKeyspace(keyspace)
      .build()
    logger.info("Cassandra session created successfully.")
    s
  }

  // Expose the session for services/controllers
  def getSession: CqlSession = session

  // Close session on shutdown
  sys.addShutdownHook {
    logger.info("Closing Cassandra session...")
    session.close()
  }
}
