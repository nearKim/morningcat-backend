package com.morningcat.infrastructure.persistence.user

import com.morningcat.domain.user.aggregate.User
import com.morningcat.domain.user.ports.UserRepository
import com.morningcat.domain.user.valueobject.EmailAddress
import com.morningcat.domain.user.valueobject.UserId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@Testcontainers
class ExposedUserRepositoryTest :
    StringSpec({

        lateinit var database: Database
        lateinit var repository: UserRepository

        beforeSpec {
            // Start PostgreSQL container
            postgres.start()

            // Connect to the database
            database =
                Database.connect(
                    url = postgres.jdbcUrl,
                    driver = "org.postgresql.Driver",
                    user = postgres.username,
                    password = postgres.password,
                )

            // Create tables
            transaction(database) {
                SchemaUtils.create(Users)
            }

            // Initialize repository
            repository = ExposedUserRepository(database)
        }

        afterSpec {
            postgres.stop()
        }

        beforeEach {
            // Clean the database before each test
            transaction(database) {
                Users.deleteAll()
            }
        }

        "should save a User aggregate and retrieve it by ID" {
            // Given
            val userId = UserId(UUID.randomUUID())
            val email = EmailAddress.create("test@example.com").getOrNull()!!
            val user =
                User.register(
                    id = userId,
                    email = email,
                    name = "Test User",
                )

            // When
            runBlocking {
                repository.save(user)
            }

            // Then
            val retrievedUser =
                runBlocking {
                    repository.findById(userId)
                }

            retrievedUser shouldNotBe null
            retrievedUser!!.id shouldBe user.id
            retrievedUser.name shouldBe user.name
            retrievedUser.getEmail() shouldBe user.getEmail()
        }

        "should retrieve a User by email" {
            // Given
            val userId = UserId(UUID.randomUUID())
            val email = EmailAddress.create("unique@example.com").getOrNull()!!
            val user =
                User.register(
                    id = userId,
                    email = email,
                    name = "Unique User",
                )

            // When
            runBlocking {
                repository.save(user)
            }

            // Then
            val retrievedUser =
                runBlocking {
                    repository.findByEmail(email)
                }

            retrievedUser shouldNotBe null
            retrievedUser!!.id shouldBe user.id
            retrievedUser.name shouldBe user.name
            retrievedUser.getEmail() shouldBe user.getEmail()
        }

        "should return null when User not found by ID" {
            // Given
            val nonExistentId = UserId(UUID.randomUUID())

            // When
            val result =
                runBlocking {
                    repository.findById(nonExistentId)
                }

            // Then
            result shouldBe null
        }

        "should return null when User not found by email" {
            // Given
            val nonExistentEmail = EmailAddress.create("nonexistent@example.com").getOrNull()!!

            // When
            val result =
                runBlocking {
                    repository.findByEmail(nonExistentEmail)
                }

            // Then
            result shouldBe null
        }

        "should update existing User when saving with same ID" {
            // Given
            val userId = UserId(UUID.randomUUID())
            val originalEmail = EmailAddress.create("original@example.com").getOrNull()!!
            val originalUser =
                User.register(
                    id = userId,
                    email = originalEmail,
                    name = "Original Name",
                )

            runBlocking {
                repository.save(originalUser)
            }

            // When - save user with same ID but different email
            val newEmail = EmailAddress.create("updated@example.com").getOrNull()!!
            val updatedUser =
                User.register(
                    id = userId,
                    email = newEmail,
                    name = "Updated Name",
                )

            runBlocking {
                repository.save(updatedUser)
            }

            // Then
            val retrievedUser =
                runBlocking {
                    repository.findById(userId)
                }

            retrievedUser shouldNotBe null
            retrievedUser!!.name shouldBe "Updated Name"
            retrievedUser.getEmail() shouldBe newEmail

            // Original email should not find any user
            val userByOldEmail =
                runBlocking {
                    repository.findByEmail(originalEmail)
                }
            userByOldEmail shouldBe null
        }
    }) {
    companion object {
        @Container
        val postgres =
            PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
                withDatabaseName("morningcat_test")
                withUsername("test")
                withPassword("test")
            }
    }
}
