package pl.newicom.dddd.test.dummy

import akka.actor.Props
import pl.newicom.dddd.aggregate.error.{AggregateRootNotInitialized, CommandHandlerNotDefined, DomainException}
import pl.newicom.dddd.aggregate.{AggregateRootActorFactory, AggregateRootLogger}
import pl.newicom.dddd.office.OfficeRef
import pl.newicom.dddd.test.dummy.DummyAggregateRoot.DummyConfig
import pl.newicom.dddd.test.dummy.DummyOfficeSpec._
import pl.newicom.dddd.test.dummy.DummyProtocol._
import pl.newicom.dddd.test.support.OfficeSpec
import pl.newicom.dddd.test.support.TestConfig.testSystem

object DummyOfficeSpec {

  implicit def actorFactory: AggregateRootActorFactory[DummyAggregateRoot] =
    AggregateRootActorFactory[DummyAggregateRoot](pc => Props(
      new DummyAggregateRoot(DummyConfig(pc, valueGenerator = () => -1)) with AggregateRootLogger[DummyEvent]
    ))
}

class DummyOfficeSpec extends OfficeSpec[DummyEvent, DummyAggregateRoot](Some(testSystem)) {

  def dummyId: DummyId = aggregateId

  "Dummy office" should {

    "create Dummy" in {
      when {
        CreateDummy(dummyId, "dummy name", "dummy description", Value(100))
      }
      .expect { c =>
        DummyCreated(c.id, c.name, c.description, c.value.value)
      }
    }

    "reject update of non-existing Dummy" in {
      when {
        ChangeName(dummyId, "some other dummy name")
      }
      .expectException[AggregateRootNotInitialized]()
    }

    "reject CreateDummy if Dummy already exists" in {
      val dId = dummyId
      given {
        CreateDummy(dId, "dummy name", "dummy description", Value(100))
      }
      when {
        CreateDummy(dId, "dummy name", "dummy description", Value(100))
      }
      .expectException[CommandHandlerNotDefined]()
    }

    "update Dummy's name" in {
      given {
        CreateDummy(dummyId, "dummy name", "dummy description", Value(100))
      }
      .when {
        ChangeName(dummyId, "some other dummy name")
      }
      .expect { c =>
        NameChanged(c.id, c.name)
      }
    }

    "handle subsequent Update command" in {
      given {
        CreateDummy(dummyId, "dummy name", "dummy description", Value(100)) &
        ChangeName(dummyId, "some other dummy name")
      }
      .when {
        ChangeName(dummyId, "yet another dummy name")
      }
      .expect { c =>
        NameChanged(c.id, c.name)
      }
    }

    "reject negative value" in {
      when {
        CreateDummy(dummyId, "dummy name", "dummy description", value = Value(-1))
      }
      .expectException[DomainException]("negative value not allowed")
    }

    "change value and name on reset" in {
      given {
        CreateDummy(dummyId, "dummy name", "dummy description", Value(100))
      }
      .when {
        Reset(dummyId, "new dummy name")
      }
      .expect { c =>
        ValueChanged(dummyId, value = 0, dummyVersion = 1) &
        NameChanged(dummyId, c.name)
      }
    }

  }

}
