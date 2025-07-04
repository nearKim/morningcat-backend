plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.arrow.core)

    testImplementation(libs.bundles.kotest)
    testImplementation(libs.kotest.property)
}

tasks.test {
    useJUnitPlatform()
}
