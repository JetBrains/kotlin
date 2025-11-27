@file:Suppress("HasPlatformType")

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider
import javax.inject.Inject

plugins {
    kotlin("jvm")
    id("project-tests-convention")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

dependencies {
    api(intellijCore())
    api(project(":core:compiler.common"))
    api(project(":kotlin-tooling-core"))
    api(project(":native:base"))

    if (kotlinBuildProperties.isKotlinNativeEnabled) {
        testImplementation(project(":kotlin-native:Interop:Indexer"))
        testImplementation(project(":native:kotlin-native-utils"))
        testImplementation(project(":kotlin-native:Interop:StubGenerator"))
        testImplementation(testFixtures(project(":native:native.tests")))
    }

    testImplementation(project(":native:external-projects-test-utils"))
    testRuntimeOnly(project(":native:analysis-api-based-test-utils"))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(testFixtures(project(":compiler:tests-common")))
    api(project(":kotlin-stdlib"))
    testImplementation(project(":kotlin-stdlib"))
    testImplementation(project(":kotlin-test"))
    testImplementation(testFixtures(project(":compiler:test-infrastructure-utils")))
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi")
    }
}

/* Configure tests */

testsJar()

val k1TestRuntimeClasspath by configurations.creating
val analysisApiRuntimeClasspath by configurations.creating

dependencies {
    k1TestRuntimeClasspath(project(":native:objcexport-header-generator-k1"))
    k1TestRuntimeClasspath(projectTests(":native:objcexport-header-generator-k1"))

    analysisApiRuntimeClasspath(project(":native:objcexport-header-generator-analysis-api"))
    analysisApiRuntimeClasspath(projectTests(":native:objcexport-header-generator-analysis-api"))
}

tasks.test.configure {
    enabled = false
}

projectTests {
    objCExportHeaderGeneratorTestTask("testK1", testDisplayNameTag = "K1") {
        classpath += k1TestRuntimeClasspath
        exclude("**/ObjCExportIntegrationTest.class")
        exclude("**/GenerateObjCExportIntegrationTestData.class")
    }

    objCExportHeaderGeneratorTestTask("testAnalysisApi", testDisplayNameTag = "AA") {
        classpath += analysisApiRuntimeClasspath
        exclude("**/ObjCExportIntegrationTest.class")
        exclude("**/GenerateObjCExportIntegrationTestData.class")
    }
}

// Dedicated tasks for generating integration test data
projectTests {
    objCExportHeaderGeneratorTestTask("generateK1IntegrationData", testDisplayNameTag = "K1") {
        classpath += k1TestRuntimeClasspath

        filter {
            includeTestsMatching("org.jetbrains.kotlin.backend.konan.tests.integration.GenerateObjCExportIntegrationTestData")
        }

        val outputDir = layout.buildDirectory.dir("integration-artifacts/k1")
        outputs.dir(outputDir)
            .withPropertyName("k1IntegrationData")

        // Configure system property to write to k1-specific directory
        val argProvider = objects.newInstance<IntegrationTestOutputDirArgumentProvider>()
        argProvider.propertyName = integrationTestOutputsDir
        argProvider.outputDir.set(outputDir)
        jvmArgumentProviders.add(argProvider)
    }

    objCExportHeaderGeneratorTestTask("generateK2IntegrationData", testDisplayNameTag = "AA") {
        classpath += analysisApiRuntimeClasspath

        filter {
            includeTestsMatching("org.jetbrains.kotlin.backend.konan.tests.integration.GenerateObjCExportIntegrationTestData")
        }

        val outputDir = layout.buildDirectory.dir("integration-artifacts/k2")
        outputs.dir(outputDir)
            .withPropertyName("k2IntegrationData")

        // Configure system property to write to k2-specific directory
        val argProvider = objects.newInstance<IntegrationTestOutputDirArgumentProvider>()
        argProvider.propertyName = integrationTestOutputsDir
        argProvider.outputDir.set(outputDir)
        jvmArgumentProviders.add(argProvider)
    }
}

// Sync task to merge K1 and K2 outputs into a single location
val prepareIntegrationTestData by tasks.registering(Sync::class) {
    from(tasks.named("generateK1IntegrationData").map {
        layout.buildDirectory.dir("integration-artifacts/k1")
    })
    from(tasks.named("generateK2IntegrationData").map {
        layout.buildDirectory.dir("integration-artifacts/k2")
    })

    into(layout.buildDirectory.dir("integration-data"))
}

// Publish the merged data as an artifact
val integrationDataElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
    }
}

artifacts {
    add(integrationDataElements.name, prepareIntegrationTestData.map {
        it.destinationDir
    })
}

tasks.check.configure {
    dependsOn("testK1")
    dependsOn("testAnalysisApi")
    dependsOn("testIntegration")
    dependsOn(":native:objcexport-header-generator-k1:check")
    dependsOn(":native:objcexport-header-generator-analysis-api:check")
}

open class IntegrationTestOutputDirArgumentProvider @Inject constructor(
    objectFactory: ObjectFactory,
) : CommandLineArgumentProvider {
    @get:Internal
    val outputDir: DirectoryProperty = objectFactory.directoryProperty()

    @get:Internal
    var propertyName: String = ""

    @get:Input
    val relativePath: String
        get() = outputDir.get().asFile.toRelativeString(outputDir.get().asFile.parentFile.parentFile)

    override fun asArguments(): Iterable<String> {
        return listOf("-D$propertyName=${outputDir.get().asFile.absolutePath}")
    }
}

tasks.withType<Test>().configureEach {
    // Skip tasks that have custom system property configuration
    if (name !in setOf("testIntegration", "generateK1IntegrationData", "generateK2IntegrationData")) {
        val provider = objects.newInstance<IntegrationTestOutputDirArgumentProvider>()
        provider.propertyName = integrationTestOutputsDir
        provider.outputDir.set(layout.buildDirectory.dir(integrationTestOutputsDir))
        jvmArgumentProviders.add(provider)
    }
}

projectTests {
    objCExportHeaderGeneratorTestTask("testIntegration", testDisplayNameTag = "testIntegration") {
        filter {
            includeTestsMatching("org.jetbrains.kotlin.backend.konan.tests.integration.ObjCExportIntegrationTest")
        }

        // Depend on the prepareIntegrationTestData task
        dependsOn(prepareIntegrationTestData)

        val integrationDataDir = layout.buildDirectory.dir("integration-data")
        inputs.files(integrationDataDir)
            .withPropertyName("integrationData")
            .withPathSensitivity(PathSensitivity.RELATIVE)

        // Use a custom argument provider for the system property
        val argProvider = objects.newInstance<IntegrationTestOutputDirArgumentProvider>()
        argProvider.propertyName = integrationTestOutputsDir
        argProvider.outputDir.set(integrationDataDir)
        jvmArgumentProviders.add(argProvider)
    }
}

val integrationTestOutputsDir = "integration-test-outputs"