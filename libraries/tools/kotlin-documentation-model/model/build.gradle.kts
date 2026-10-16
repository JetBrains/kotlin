plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("test-inputs-check")
}

description = "Kotlin Documentation Model (KDM): core model classes"

dependencies {
    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.json)

    testImplementation(kotlinTest("junit5"))
    testImplementation(kotlin("reflect"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(libs.kotlinx.schema.generator.json)
}

projectTests {
    testData(project.isolated, "src/test/resources")

    // kotlinx-schema-generator-json (used by KdJsonSchemaTest) ships JDK 17 bytecode; testTask() defaults to a
    // JDK 17 launcher.
    testTask {
        systemProperty(
            "kdm.test.overwrite",
            project.providers.gradleProperty("kdm.test.overwrite")
        )
    }
}
