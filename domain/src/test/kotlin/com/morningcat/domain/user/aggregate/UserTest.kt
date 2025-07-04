package com.morningcat.domain.user.aggregate

import com.morningcat.domain.user.valueobject.EmailAddress
import com.morningcat.domain.user.valueobject.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class UserTest :
    StringSpec({

        val userId = UserId.generate()
        val email = EmailAddress.create("user@example.com").getOrNull()!!

        "should register new user with factory method" {
            val user =
                User.register(
                    id = userId,
                    email = email,
                    name = "John Doe",
                )

            user.id shouldBe userId
            user.getEmail() shouldBe email
            user.name shouldBe "John Doe"
        }

        "should trim user name" {
            val user =
                User.register(
                    id = userId,
                    email = email,
                    name = "  John Doe  ",
                )

            user.name shouldBe "John Doe"
        }

        "should reject blank name" {
            shouldThrow<IllegalArgumentException> {
                User.register(
                    id = userId,
                    email = email,
                    name = "",
                )
            }.message shouldBe "User name cannot be blank"

            shouldThrow<IllegalArgumentException> {
                User.register(
                    id = userId,
                    email = email,
                    name = "   ",
                )
            }.message shouldBe "User name cannot be blank"
        }

        "should change email successfully" {
            val user =
                User.register(
                    id = userId,
                    email = email,
                    name = "John Doe",
                )

            val newEmail = EmailAddress.create("newemail@example.com").getOrNull()!!
            user.changeEmail(newEmail)

            user.getEmail() shouldBe newEmail
        }

        "should reject changing to same email" {
            val user =
                User.register(
                    id = userId,
                    email = email,
                    name = "John Doe",
                )

            shouldThrow<IllegalArgumentException> {
                user.changeEmail(email)
            }.message shouldBe "New email must be different from current email"
        }

        "should maintain immutable properties after email change" {
            val user =
                User.register(
                    id = userId,
                    email = email,
                    name = "John Doe",
                )

            val newEmail = EmailAddress.create("newemail@example.com").getOrNull()!!
            user.changeEmail(newEmail)

            user.id shouldBe userId // ID unchanged
            user.name shouldBe "John Doe" // Name unchanged
        }

        "should have identity based on UserId" {
            val user1 =
                User.register(
                    id = userId,
                    email = email,
                    name = "John Doe",
                )

            val user2 =
                User.register(
                    id = userId,
                    email = EmailAddress.create("different@example.com").getOrNull()!!,
                    name = "Different Name",
                )

            user1 shouldBe user2 // Same ID means same user
            user1.hashCode() shouldBe user2.hashCode()
        }

        "should have different identity for different UserIds" {
            val user1 =
                User.register(
                    id = UserId.generate(),
                    email = email,
                    name = "John Doe",
                )

            val user2 =
                User.register(
                    id = UserId.generate(),
                    email = email,
                    name = "John Doe",
                )

            user1 shouldNotBe user2 // Different IDs mean different users
        }
    })
