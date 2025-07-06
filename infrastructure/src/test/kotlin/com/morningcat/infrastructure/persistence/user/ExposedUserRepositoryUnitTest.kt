package com.morningcat.infrastructure.persistence.user

import com.morningcat.domain.user.aggregate.User
import com.morningcat.domain.user.valueobject.EmailAddress
import com.morningcat.domain.user.valueobject.UserId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class ExposedUserRepositoryUnitTest :
    StringSpec({

        val database =
            Database.connect(
                url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                driver = "org.h2.Driver",
                user = "sa",
                password = "",
            )

        beforeSpec {
            transaction(database) {
                SchemaUtils.create(Users)
            }
        }

        beforeEach {
            transaction(database) {
                Users.deleteAll()
            }
        }

        "ExposedUserRepository compiles and basic structure is correct" {
            // Given
            val repository = ExposedUserRepository(database)

            // When
            val userId = UserId(UUID.randomUUID())
            val email = EmailAddress.create("test@example.com").getOrNull()!!
            val user =
                User.register(
                    id = userId,
                    email = email,
                    name = "Test User",
                )

            // Then - just verify the code compiles and runs
            repository.save(user)

            val retrieved = repository.findById(userId)
            retrieved shouldNotBe null
            retrieved?.id shouldBe userId
            retrieved?.name shouldBe "Test User"
            retrieved?.getEmail()?.value shouldBe "test@example.com"
        }
    })
