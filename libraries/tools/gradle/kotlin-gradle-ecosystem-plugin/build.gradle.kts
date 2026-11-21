import gradle.addKgpGradleApiDependency
import kotlinx.validation.KotlinApiBuildTask

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    id("gradle-plugin-common-configuration")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("test-inputs-check")
    `java-test-fixtures`
}

gradlePlugin {
    plugins {
        create("kotlin-ecosystem-plugin") {
            id = "org.jetbrains.kotlin.ecosystem"
            displayName = "Kotlin Ecosystem plugin"
            description = "Gradle settings plugin providing project wide Kotlin configuration"
            implementationClass = "org.jetbrains.kotlin.gradle.ecosystem.KotlinEcosystemPlugin"
        }
    }
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
    commonApi(project(":kotlin-gradle-plugin"))

    testImplementation(kotlinTest("junit5"))

    addKgpGradleApiDependency("testFixturesCompileOnly")
}

projectTests {
    testTask()
}

components.named<AdhocComponentWithVariants>("java").configure {
    // disable publication of test fixtures
    java.sourceSets.testFixtures.configure {
        setOf(
            apiElementsConfigurationName,
            runtimeElementsConfigurationName,
        ).forEach { testFixtureConfigurationName ->
            withVariantsFromConfiguration(configurations[testFixtureConfigurationName]) {
                skip()
            }
        }
    }
}

tasks.named<KotlinApiBuildTask>("apiBuild") {
    inputJar.value(tasks.named<org.gradle.jvm.tasks.Jar>("embeddableJar").flatMap { it.archiveFile })
}
