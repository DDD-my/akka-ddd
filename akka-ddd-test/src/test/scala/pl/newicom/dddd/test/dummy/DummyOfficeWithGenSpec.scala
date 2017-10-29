package pl.newicom.dddd.test.dummy

import akka.actor.Props
import org.scalacheck.Gen
import pl.newicom.dddd.aggregate.{AggregateRootActorFactory, EntityId}
import pl.newicom.dddd.test.dummy.DummyProtocol._
import pl.newicom.dddd.test.support.OfficeSpec
import pl.newicom.dddd.test.support.TestConfig._
import DummyOfficeWithGenSpec._
import pl.newicom.dddd.aggregate.error.DomainException
import pl.newicom.dddd.office.OfficeRef
import pl.newicom.dddd.test.dummy.DummyAggregateRoot.DummyConfig

object DummyOfficeWithGenSpec {

  implicit def actorFactory: AggregateRootActorFactory[DummyAggregateRoot] =
    AggregateRootActorFactory[DummyAggregateRoot](pc => Props(new DummyAggregateRoot(DummyConfig(pc))))
}

class DummyOfficeWithGenSpec extends OfficeSpec[DummyEvent, DummyAggregateRoot](Some(testSystem)) {

  def dummyOffice: OfficeRef = officeUnderTest

  def dummyId: EntityId = aggregateId

  //
  // Command generators
  //
  implicit def create: Gen[CreateDummy] = for {
    name <- Gen.alphaStr
    description <- Gen.alphaStr
    value <- Gen.choose(1, 1000)
  } yield {
    CreateDummy(dummyId, name, description, value)
  }
  implicit def changeName: Gen[ChangeName] = for { name <- Gen.alphaStr } yield ChangeName(dummyId, name)
  implicit def generateValue: Gen[GenerateValue] = _aggregateIdGen.flatMap(GenerateValue(_))

  "Dummy office" should {
    /**
     * Commands are generated (see generators above).
     * The command under test (created inside When clause)
     * is accessible from inside Then/expect clause.
     *
     * No need to define val members for name, description, ...
     * No need to define concrete values for name, description, ...
     */
    "create Dummy" in {
      when {
        a [CreateDummy]
      }
      .expect { c =>
        DummyCreated(c.id, c.name, c.description, c.value)
      }
    }

    /**
     * Events triggered by Given command(s) are accessible from inside When clause.
     * Gen.suchThat can be used to configure command generator inside test body.
     */
    "update Dummy's name" in {
      given {
        a [CreateDummy]
      }
      .when { implicit hist =>
        a [ChangeName] suchThat (_.name != past[DummyCreated].name)
      }
      .expect { c =>
        NameChanged(c.id, c.name)
      }
    }

    /**
      * Multiple commands in When section are supported.
      * 'expect' in Then section is able to handle multiple expected events.
      */
    "update Dummy's value twice" in {
      given {
        a [CreateDummy]
      }
      .when {
        ChangeValue(dummyId, 1) & ChangeValue(dummyId, 2)
      }
      .expect { c =>
        ValueChanged(dummyId, 1, 1) & ValueChanged(dummyId, 2, 2)
      }
    }

    /**
     * No problem with two or more commands inside Given clause.
     */
    "handle subsequent Update command" in {
      given(
        a_list_of [CreateDummy, ChangeName]
      )
      .when {
        a [ChangeName]
      }
      .expect { c =>
        NameChanged(c.id, c.name)
      }
    }

    /**
     * Events triggered by Given command(s) are accessible from inside Then/expect clause.
     */
    "confirm generated value" in {
      given(
        a_list_of [CreateDummy, GenerateValue]
      )
      .when { implicit hist =>
        ConfirmGeneratedValue(dummyId, past[ValueGenerated].confirmationToken)
      }
      .expect { implicit c =>
        ValueChanged(c.id, past[ValueGenerated].value, dummyVersion = 1)
      }
    }

    /**
     * Gen.map can be used to modify generated command.
     */
    "reject negative value" in {
      when {
        a [CreateDummy] map (_ copy(value = -1))
        // alternatively:
        //arbitraryOf[CreateDummy](_ copy(value = -1))
      }
      .expectException[DomainException]("negative value not allowed")
    }
  }


}

