plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

application {
    mainClass.set("com.morningcat.bootstrap.ApplicationKt")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation(project(":infrastructure"))
    implementation(project(":presentation"))
    implementation(libs.kotlin.stdlib)

    // DI - Koin
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    // Configuration
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.yaml)

    // Logging
    implementation(libs.logback.classic)

    testImplementation(libs.bundles.kotest)
    testImplementation(libs.koin.test)
}

tasks.test {
    useJUnitPlatform()
}
