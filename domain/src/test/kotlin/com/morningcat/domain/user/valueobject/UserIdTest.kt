package com.morningcat.domain.user.valueobject

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID

class UserIdTest :
    StringSpec({

        "should create UserId from UUID" {
            val uuid = UUID.randomUUID()
            val userId = UserId(uuid)

            userId.value shouldBe uuid
        }

        "should generate new UserId with random UUID" {
            val userId1 = UserId.generate()
            val userId2 = UserId.generate()

            userId1 shouldNotBe userId2
            userId1.value shouldNotBe userId2.value
        }

        "should create UserId from string" {
            val uuidString = UUID.randomUUID().toString()
            val userId = UserId.fromString(uuidString)

            userId.value.toString() shouldBe uuidString
        }

        "should return UUID string representation" {
            val uuid = UUID.randomUUID()
            val userId = UserId(uuid)

            userId.toString() shouldBe uuid.toString()
        }

        "should have value semantics based on UUID" {
            val uuid = UUID.randomUUID()
            val userId1 = UserId(uuid)
            val userId2 = UserId(uuid)

            userId1 shouldBe userId2
            userId1.hashCode() shouldBe userId2.hashCode()
        }
    })
