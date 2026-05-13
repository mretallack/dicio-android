package org.stypox.dicio.skills.homeassistant

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.stypox.dicio.sentences.Sentences

class VacuumCommandTest : StringSpec({

    "parse 'vacuum the kitchen'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "vacuum the kitchen"
        val (score, inputData) = data.score(TestSkillContext(input), input)

        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.VacuumRoom>()
        val vacuum = inputData as Sentences.HomeAssistant.VacuumRoom
        vacuum.roomNames?.trim() shouldBe "kitchen"
    }

    "parse 'clean the living room'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "clean the living room"
        val (score, inputData) = data.score(TestSkillContext(input), input)

        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.VacuumRoom>()
        val vacuum = inputData as Sentences.HomeAssistant.VacuumRoom
        vacuum.roomNames?.trim() shouldBe "living room"
    }

    "parse 'hoover the hallway'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "hoover the hallway"
        val (score, inputData) = data.score(TestSkillContext(input), input)

        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.VacuumRoom>()
        val vacuum = inputData as Sentences.HomeAssistant.VacuumRoom
        vacuum.roomNames?.trim() shouldBe "hallway"
    }

    "parse 'vacuum kitchen and hallway'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "vacuum kitchen and hallway"
        val (score, inputData) = data.score(TestSkillContext(input), input)

        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.VacuumRoom>()
        val vacuum = inputData as Sentences.HomeAssistant.VacuumRoom
        vacuum.roomNames?.trim() shouldBe "kitchen"
        vacuum.roomNames2?.trim() shouldBe "hallway"
    }

    "parse 'stop the vacuum'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "stop the vacuum"
        val (score, inputData) = data.score(TestSkillContext(input), input)

        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.VacuumStop>()
    }

    "parse 'send the robot home'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "send the robot home"
        val (score, inputData) = data.score(TestSkillContext(input), input)

        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.VacuumDock>()
    }

    "parse 'start the vacuum'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "start the vacuum"
        val (score, inputData) = data.score(TestSkillContext(input), input)

        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.VacuumStart>()
    }

    "parse 'pause the hoover'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "pause the hoover"
        val (score, inputData) = data.score(TestSkillContext(input), input)

        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.VacuumPause>()
    }

    "parse 'dock the vacuum'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "dock the vacuum"
        val (score, inputData) = data.score(TestSkillContext(input), input)

        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.VacuumDock>()
    }

    "does not conflict with set_state_on for 'turn vacuum on'" {
        val data = Sentences.HomeAssistant["en"]!!
        val input = "turn vacuum on"
        val (score, inputData) = data.score(TestSkillContext(input), input)

        inputData.shouldBeInstanceOf<Sentences.HomeAssistant.SetStateOn>()
    }
})
