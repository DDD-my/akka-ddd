package pl.newicom.dddd.messaging.event

import akka.actor.Actor
import pl.newicom.dddd.Eventsourced
import pl.newicom.dddd.messaging.event.EventStreamSubscriber._

object EventStreamSubscriber {

  trait DemandCallback {
    def onEventProcessed()
  }

  case class DemandConfig(subscriberCapacity: Int, initialDemand: Int)
}

trait EventStreamSubscriber {
  this: Actor =>

  /**
   * Subscribes this actor (the subscriber) to given event stream.
   * The subscriber will receive events as [[pl.newicom.dddd.messaging.event.EventMessageEntry]] messages.
   *
   * @param fromPosExcl if provided Subscriber will be receiving events
   *                              from given position (exclusively)
    * @return callback that the subscriber should invoke after processing an event.
   */
  def subscribe(observable: Eventsourced, fromPosExcl: Option[Long], demandConfig: DemandConfig): DemandCallback


}
