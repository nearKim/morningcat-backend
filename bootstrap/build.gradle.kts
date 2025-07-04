plugins {
    kotlin("jvm")
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
    implementation(kotlin("stdlib"))
    
    // DI - Koin
    implementation("io.insert-koin:koin-core:4.0.0")
    implementation("io.insert-koin:koin-ktor:4.0.0")
    implementation("io.insert-koin:koin-logger-slf4j:4.0.0")
    
    // Configuration
    implementation("com.sksamuel.hoplite:hoplite-core:2.9.0")
    implementation("com.sksamuel.hoplite:hoplite-yaml:2.9.0")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.12")
    
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.insert-koin:koin-test:4.0.0")
}

tasks.test {
    useJUnitPlatform()
}