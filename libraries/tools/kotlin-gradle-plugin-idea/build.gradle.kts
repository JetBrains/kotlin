import plugins.KotlinBuildPublishingPlugin.Companion.ADHOC_COMPONENT_NAME

plugins {
    kotlin("jvm")
    `java-test-fixtures`
    `maven-publish`
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("project-tests-convention")
    id("test-inputs-check")
}

configureKotlinCompileTasksGradleCompatibility()

kotlin.sourceSets.configureEach {
    languageSettings.optIn("org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi")
}

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly(kotlin("stdlib", coreDepsVersion))
    api(project(":kotlin-tooling-core"))
    api(project(":kotlin-gradle-plugin-annotations"))
    testImplementation(gradleApi())
    testImplementation(gradleKotlinDsl())
    testImplementation(project(":kotlin-gradle-plugin"))
    testImplementation(project(":kotlin-gradle-plugin-idea-proto"))
    testImplementation(kotlin("stdlib", coreDepsVersion))
    testImplementation(kotlin("test-junit5", coreDepsVersion))
    testImplementation(libs.junit.jupiter.params)

    testImplementation("org.reflections:reflections:0.10.2") {
        because("Tests on the object graph are performed. This library will find implementations of interfaces at runtime")
    }

    testFixturesImplementation(gradleApi())
    testFixturesImplementation(gradleKotlinDsl())
    testFixturesImplementation(project(":kotlin-tooling-core"))
    testFixturesImplementation(project(":kotlin-gradle-plugin-idea-proto"))
    testFixturesImplementation(kotlin("stdlib", coreDepsVersion))
    testFixturesImplementation(kotlin("test", coreDepsVersion)) // no test annotations, only assertions are needed
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
    testTask(jUnitMode = JUnitMode.JUnit5)
}
