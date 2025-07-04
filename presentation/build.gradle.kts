plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":application"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.arrow.core)

    // Ktor Server
    implementation(libs.bundles.ktor.server)

    // Logging
    implementation(libs.logback.classic)

    // API Documentation
    implementation(libs.ktor.server.swagger)

    testImplementation(libs.bundles.kotest)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}
