import gradle.addKgpGradleApiDependency
import plugins.KotlinBuildPublishingPlugin.Companion.ADHOC_COMPONENT_NAME

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    `java-test-fixtures`
    `maven-publish`
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("test-inputs-check")
}

configureKotlinCompileTasksGradleCompatibility()

kotlin {
    coreLibrariesVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi")
    }
}

dependencies {
    // 'kotlin.coreLibrariesVersion' usage caused by KT-71443
    compileOnly(kotlin("stdlib", kotlin.coreLibrariesVersion))
    api(project(":kotlin-tooling-core"))
    api(project(":kotlin-gradle-plugin-annotations"))

    addKgpGradleApiDependency("testCompileOnly")

    testImplementation(project(":kotlin-gradle-plugin"))
    testImplementation(project(":kotlin-gradle-plugin-idea-proto"))
    testImplementation(kotlin("stdlib"))
    testImplementation(kotlin("reflect"))
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.junit.jupiter.params)
    testImplementation("org.reflections:reflections:0.10.2") {
        because("Tests on the object graph are performed. This library will find implementations of interfaces at runtime")
    }
    testRuntimeOnly(gradleApi())

    addKgpGradleApiDependency("testFixturesCompileOnly")
    testFixturesImplementation(project(":kotlin-tooling-core"))
    testFixturesImplementation(project(":kotlin-gradle-plugin-idea-proto"))
    // 'kotlin.coreLibrariesVersion' usage caused by KT-71443
    testFixturesImplementation(kotlin("stdlib", kotlin.coreLibrariesVersion))
    testFixturesImplementation(kotlin("reflect", kotlin.coreLibrariesVersion))
    testFixturesImplementation(kotlin("test", kotlin.coreLibrariesVersion)) // no test annotations, only assertions are needed
}


publish(moduleMetadata = true) {
    fun ConfigurationVariantDetails.skipUnpublishable() {
        if (configurationVariant.artifacts.any { JavaBasePlugin.UNPUBLISHABLE_VARIANT_ARTIFACTS.contains(it.type) }) {
            skip()
        }
    }

    suppressAllPomMetadataWarnings()

    val kotlinLibraryComponent = components[ADHOC_COMPONENT_NAME] as AdhocComponentWithVariants

    kotlinLibraryComponent.addVariantsFromConfiguration(configurations.testFixturesApiElements.get()) {
        skipUnpublishable()
        mapToMavenScope("compile")
        mapToOptional()
    }

    kotlinLibraryComponent.addVariantsFromConfiguration(configurations.testFixturesRuntimeElements.get()) {
        skipUnpublishable()
        mapToMavenScope("runtime")
        mapToOptional()
    }
}

javadocJar()
sourcesJar()

apiValidation {
    nonPublicMarkers += "org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi"
}

tasks {
    apiBuild {
        inputJar.value(jar.flatMap { it.archiveFile })
    }
}

projectTests {
    testTask()
}
