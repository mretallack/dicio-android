package org.stypox.dicio.skills.homeassistant

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.stypox.dicio.sentences.Sentences

class VacuumRoomMatchingTest : StringSpec({
    val skill = HomeAssistantSkill(HomeAssistantInfo, Sentences.HomeAssistant["en"]!!)

    val rooms = listOf(
        HomeAssistantSkill.RoomInfo(1, "Hallway"),
        HomeAssistantSkill.RoomInfo(2, "Kitchen"),
        HomeAssistantSkill.RoomInfo(3, "Utility Room"),
        HomeAssistantSkill.RoomInfo(4, "Living Room"),
    )

    fun findRoom(spoken: String): HomeAssistantSkill.RoomInfo? {
        return skill.matchRoomName(spoken, rooms)
    }

    "exact match - kitchen" {
        findRoom("kitchen")?.name shouldBe "Kitchen"
        findRoom("kitchen")?.id shouldBe 2
    }

    "exact match - living room" {
        findRoom("living room")?.name shouldBe "Living Room"
        findRoom("living room")?.id shouldBe 4
    }

    "exact match - hallway" {
        findRoom("hallway")?.name shouldBe "Hallway"
        findRoom("hallway")?.id shouldBe 1
    }

    "contains match - utility matches Utility Room" {
        findRoom("utility")?.name shouldBe "Utility Room"
    }

    "contains match - living matches Living Room" {
        findRoom("living")?.name shouldBe "Living Room"
    }

    "no match - garage" {
        findRoom("garage") shouldBe null
    }

    "no match - office" {
        findRoom("office") shouldBe null
    }

    "case insensitive - KITCHEN" {
        findRoom("KITCHEN")?.name shouldBe "Kitchen"
    }
})
