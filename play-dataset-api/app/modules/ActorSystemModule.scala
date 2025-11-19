package modules

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Provides}
import javax.inject.Singleton
import play.api.Configuration

class RedisModule extends AbstractModule {
  
  @Provides
  @Singleton
  def provideActorSystem(): ActorSystem = {
    ActorSystem("play-redis-system")
  }
}