package com.beachape.actors

import akka.actor.ActorSystem

/**
 * Just singleton object holding a reference to an ActorSystem
 */
object MainActorSystem {
  implicit val system = ActorSystem("akanoriSystem")
}
