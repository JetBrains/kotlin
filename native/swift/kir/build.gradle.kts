import org.jetbrains.kotlin.testFederation.SmokeTestConfig
import org.jetbrains.kotlin.testFederation.TemporaryTestFederationApi
import org.jetbrains.kotlin.testFederation.smokeTestConfig

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("generated-sources")
}

description = "Kotlin Intermediate Representation"

dependencies {
    compileOnly(kotlinStdlib())

    if (kotlinBuildProperties.isInIdeaSync.get()) {
        compileOnly(project("tree-generator")) // Provided, so that IDEA can recognize references to this module in KDoc.
    }
}

sourceSets {
    "main" { projectDefault() }
}

generatedSourcesTask(
    taskName = "generateTree",
    generatorProject = ":native:swift:kir:tree-generator",
    generatorMainClass = "org.jetbrains.kotlin.kir.tree.generator.MainKt",
)

publish()

runtimeJar()
sourcesJar()
javadocJar()

tasks.test.configure {
    @OptIn(TemporaryTestFederationApi::class)
    smokeTestConfig = SmokeTestConfig.RunAllTests
}
