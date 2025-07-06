import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.arrow.core)

    // Database
    implementation(libs.bundles.exposed)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)

    // HTTP clients for external APIs
    implementation(libs.bundles.ktor.client)

    // Caching
    implementation(libs.lettuce.core)

    // Email
    implementation(libs.sendgrid.java)
    
    // Push Notifications
    implementation(libs.firebase.admin)

    // Template Engine
    implementation(libs.freemarker)

    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.h2database)
    testImplementation(libs.mockwebserver)
}

tasks.test {
    useJUnitPlatform()
}
