plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

kotlin {
    explicitApi()
}

configureKotlinCompileTasksGradleCompatibility()

publish()

standardPublicJars()

dependencies {
    // remove stdlib dependency from api artifact in order not to affect the dependencies of the user project
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly(kotlin("stdlib", coreDepsVersion))

    testImplementation(platform(libs.junit.bom))
    testImplementation(kotlin("stdlib", coreDepsVersion))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
