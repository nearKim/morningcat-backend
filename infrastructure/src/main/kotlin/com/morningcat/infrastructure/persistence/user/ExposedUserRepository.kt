package com.morningcat.infrastructure.persistence.user

import com.morningcat.domain.user.aggregate.User
import com.morningcat.domain.user.ports.UserRepository
import com.morningcat.domain.user.valueobject.EmailAddress
import com.morningcat.domain.user.valueobject.UserId
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant

class ExposedUserRepository(
    private val database: Database,
) : UserRepository {
    override suspend fun findById(id: UserId): User? =
        dbQuery {
            Users
                .selectAll()
                .where { Users.id eq id.value }
                .singleOrNull()
                ?.toUser()
        }

    override suspend fun findByEmail(email: EmailAddress): User? =
        dbQuery {
            Users
                .selectAll()
                .where { Users.email eq email.value }
                .singleOrNull()
                ?.toUser()
        }

    override suspend fun save(user: User) {
        dbQuery {
            val existingUser =
                Users
                    .selectAll()
                    .where { Users.id eq user.id.value }
                    .singleOrNull()

            if (existingUser != null) {
                // Update existing user
                Users.update({ Users.id eq user.id.value }) {
                    it[email] = user.getEmail().value
                    it[name] = user.name
                    it[updatedAt] = Instant.now()
                }
            } else {
                // Insert new user
                Users.insert {
                    it[id] = user.id.value
                    it[email] = user.getEmail().value
                    it[name] = user.name
                    it[createdAt] = Instant.now()
                    it[updatedAt] = Instant.now()
                }
            }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO, database) { block() }

    private fun ResultRow.toUser(): User {
        val userId = UserId(this[Users.id])
        val email =
            EmailAddress.create(this[Users.email]).getOrNull()
                ?: throw IllegalStateException("Invalid email in database: ${this[Users.email]}")
        val name = this[Users.name]

        return User.register(
            id = userId,
            email = email,
            name = name,
        )
    }
}
