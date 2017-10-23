package pl.newicom.dddd.office
import akka.actor.ActorRef
import akka.util.Timeout
import pl.newicom.dddd.aggregate.error.{CommandHandlerNotDefined, QueryHandlerNotDefined}
import pl.newicom.dddd.aggregate.{Command, Query}
import pl.newicom.dddd.delivery.protocol.Processed
import pl.newicom.dddd.office.OfficeRefStub.{CommandHandler, QueryHandler}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.Failure

object OfficeRefStub {
  type CommandHandler = PartialFunction[Command, Processed]
  type QueryHandler[A <: Query] = PartialFunction[A, A#R]

  def apply(officeId: OfficeId, cmdHandler: CommandHandler): OfficeRefLike =
    new OfficeRefStub(officeId, cmdHandler, null)

  def apply(officeId: OfficeId, cmdHandler: CommandHandler, qHandler: QueryHandler[Query]): OfficeRefLike =
    new OfficeRefStub(officeId, cmdHandler, qHandler)
}

class OfficeRefStub(val officeId: OfficeId, cmdHandler: CommandHandler, qHandler: QueryHandler[Query]) extends OfficeRefLike {

  def ?(command: Command)(implicit sender: ActorRef, ex: ExecutionContext, t: Timeout): Future[Processed] =
    Future.successful(
      cmdHandler.applyOrElse(
        command,
        (c: Command) => Processed(Failure(CommandHandlerNotDefined(c.getClass.getSimpleName)))))

  def ?(query: Query)(implicit ex: ExecutionContext, t: Timeout, ct: ClassTag[query.R], sender: ActorRef): Future[query.R] =
    if (qHandler == null)
      Future(qHandler(query)).mapTo[query.R]
    else
      Future.failed(new QueryHandlerNotDefined(query.getClass.getSimpleName))
}
