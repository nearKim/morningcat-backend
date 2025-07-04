package com.morningcat.domain.user.valueobject

import com.morningcat.domain.shared.error.DomainError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class EmailAddressTest :
    StringSpec({

        "should create valid email address" {
            val email = "user@example.com"
            val result = EmailAddress.create(email)

            result.shouldBeRight { emailAddress ->
                emailAddress.value shouldBe email
            }
        }

        "should trim whitespace from email" {
            val email = "  user@example.com  "
            val result = EmailAddress.create(email)

            result.shouldBeRight { emailAddress ->
                emailAddress.value shouldBe "user@example.com"
            }
        }

        "should accept various valid email formats" {
            val validEmails =
                listOf(
                    "simple@example.com",
                    "user.name@example.com",
                    "user+tag@example.com",
                    "user_name@example.com",
                    "123@example.com",
                    "user@subdomain.example.com",
                    "user@example.co.uk",
                )

            validEmails.forEach { email ->
                EmailAddress.create(email).shouldBeRight()
            }
        }

        "should reject empty email" {
            val result = EmailAddress.create("")

            result.shouldBeLeft(DomainError.InvalidEmail("", "Email address cannot be empty"))
        }

        "should reject blank email" {
            val result = EmailAddress.create("   ")

            result.shouldBeLeft(DomainError.InvalidEmail("   ", "Email address cannot be empty"))
        }

        "should reject invalid email formats" {
            val invalidEmails =
                listOf(
                    "notanemail",
                    "@example.com",
                    "user@",
                    "user@@example.com",
                    "user@example",
                    "user example@example.com",
                    "user@.com",
                    "user@example..com",
                )

            invalidEmails.forEach { email ->
                val result = EmailAddress.create(email)
                result.shouldBeLeft(DomainError.InvalidEmail(email, "Invalid email format"))
            }
        }

        "should validate email format through factory method" {
            val result = EmailAddress.create("invalid")
            result.isLeft() shouldBe true
        }

        "should have value semantics" {
            val email1 = EmailAddress.create("user@example.com").getOrNull()!!
            val email2 = EmailAddress.create("user@example.com").getOrNull()!!

            email1 shouldBe email2
            email1.hashCode() shouldBe email2.hashCode()
        }
    })
