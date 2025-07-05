package com.morningcat.infrastructure.provider.task

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.morningcat.domain.common.error.ProviderError
import com.morningcat.domain.content.entity.Task
import com.morningcat.domain.content.ports.TaskProvider
import com.morningcat.domain.user.aggregate.User
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TodoistAdapter(
    private val httpClient: HttpClient,
    private val apiToken: String,
    private val baseUrl: String = "https://api.todoist.com/rest/v2"
) : TaskProvider {
    
    override suspend fun getTasks(
        user: User,
        date: LocalDate
    ): Either<ProviderError, List<Task>> {
        return try {
            // Todoist filter format for specific date
            val filter = URLEncoder.encode("due: ${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}", "UTF-8")
            
            val response = httpClient.get("$baseUrl/tasks") {
                parameter("filter", filter)
                headers {
                    append(HttpHeaders.Authorization, "Bearer $apiToken")
                }
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    val todoistTasks = try {
                        response.body<List<TodoistTask>>()
                    } catch (e: Exception) {
                        return ProviderError.InvalidResponse("Failed to parse tasks: ${e.message}").left()
                    }
                    
                    val tasks = todoistTasks.mapNotNull { todoistTask ->
                        parseTask(todoistTask)
                    }
                    
                    tasks.right()
                }
                HttpStatusCode.Unauthorized -> {
                    ProviderError.AuthenticationError("Todoist").left()
                }
                HttpStatusCode.TooManyRequests -> {
                    val resetTime = response.headers["X-RateLimit-Reset"]?.toLongOrNull()
                    ProviderError.RateLimitExceeded(resetTime).left()
                }
                HttpStatusCode.InternalServerError,
                HttpStatusCode.BadGateway,
                HttpStatusCode.ServiceUnavailable,
                HttpStatusCode.GatewayTimeout -> {
                    ProviderError.ServiceUnavailable("Todoist").left()
                }
                else -> {
                    ProviderError.ServiceUnavailable("Todoist").left()
                }
            }
        } catch (e: Exception) {
            ProviderError.NetworkError(e.message ?: "Unknown error").left()
        }
    }
    
    private fun parseTask(todoistTask: TodoistTask): Task? {
        return try {
            val title = todoistTask.content.trim()
            if (title.isBlank()) return null // Skip tasks without title
            
            val description = when {
                todoistTask.description.isNullOrBlank() -> null
                else -> todoistTask.description.trim()
            }
            
            val dueDate = todoistTask.due?.datetime?.let { dateTimeStr ->
                try {
                    ZonedDateTime.parse(dateTimeStr).toLocalDateTime()
                } catch (e: Exception) {
                    // Try parsing as date only
                    todoistTask.due.date?.let { dateStr ->
                        LocalDate.parse(dateStr).atStartOfDay()
                    }
                }
            }
            
            val priority = mapPriority(todoistTask.priority)
            
            Task(
                title = title,
                description = description,
                dueDate = dueDate,
                priority = priority,
                isCompleted = todoistTask.isCompleted
            )
        } catch (e: Exception) {
            null // Skip invalid tasks
        }
    }
    
    private fun mapPriority(todoistPriority: Int): Task.Priority {
        return when (todoistPriority) {
            4 -> Task.Priority.HIGH
            3, 2 -> Task.Priority.MEDIUM
            else -> Task.Priority.LOW
        }
    }
    
    @Serializable
    private data class TodoistTask(
        val id: String,
        val content: String = "",  // Default to empty string if missing
        val description: String? = null,
        val due: DueDate? = null,
        val priority: Int,
        @SerialName("is_completed")
        val isCompleted: Boolean
    )
    
    @Serializable
    private data class DueDate(
        val date: String? = null,
        val datetime: String? = null,
        val timezone: String? = null
    )
}