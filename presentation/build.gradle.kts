plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.0.21"
}

dependencies {
    implementation(project(":application"))
    implementation(kotlin("stdlib"))
    implementation("io.arrow-kt:arrow-core:1.2.4")
    
    // Ktor Server
    implementation("io.ktor:ktor-server-core:3.0.1")
    implementation("io.ktor:ktor-server-netty:3.0.1")
    implementation("io.ktor:ktor-server-content-negotiation:3.0.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.1")
    implementation("io.ktor:ktor-server-cors:3.0.1")
    implementation("io.ktor:ktor-server-auth:3.0.1")
    implementation("io.ktor:ktor-server-auth-jwt:3.0.1")
    implementation("io.ktor:ktor-server-status-pages:3.0.1")
    implementation("io.ktor:ktor-server-call-logging:3.0.1")
    implementation("io.ktor:ktor-server-websockets:3.0.1")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.12")
    
    // API Documentation
    implementation("io.ktor:ktor-server-swagger:3.0.1")
    
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.kotest.extensions:kotest-assertions-arrow:1.4.0")
    testImplementation("io.ktor:ktor-server-tests:3.0.1")
    testImplementation("io.mockk:mockk:1.13.13")
}

tasks.test {
    useJUnitPlatform()
}