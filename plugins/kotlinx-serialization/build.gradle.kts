import org.gradle.api.publish.internal.PublicationInternal
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import plugins.KotlinBuildPublishingPlugin.Companion.ADHOC_COMPONENT_NAME
import plugins.configureKotlinPomAttributes

description = "Kotlin Serialization Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("d8-configuration")
    id("java-test-fixtures")
    id("compiler-tests-convention")
}

val jsonJsIrRuntimeForTests: Configuration by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
    }
}

val coreJsIrRuntimeForTests: Configuration by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
    }
}

dependencies {
    embedded(project(":kotlinx-serialization-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlinx-serialization-compiler-plugin.k1")) { isTransitive = false }
    embedded(project(":kotlinx-serialization-compiler-plugin.k2")) { isTransitive = false }
    embedded(project(":kotlinx-serialization-compiler-plugin.backend")) { isTransitive = false }
    embedded(project(":kotlinx-serialization-compiler-plugin.cli")) { isTransitive = false }

    testFixturesApi(project(":compiler:backend"))
    testFixturesApi(project(":compiler:cli"))
    testFixturesApi(project(":kotlinx-serialization-compiler-plugin.cli"))

    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-compiler-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(project(":compiler:fir:plugin-utils"))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))
    testFixturesApi(testFixtures(project(":js:js.tests")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-fir")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-impl-base")))
    testFixturesApi(testFixtures(project(":analysis:low-level-api-fir")))

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testFixturesApi(project(":kotlinx-serialization-compiler-plugin.common"))
    testFixturesApi(project(":kotlinx-serialization-compiler-plugin.k1"))
    testFixturesApi(project(":kotlinx-serialization-compiler-plugin.k2"))
    testFixturesApi(project(":kotlinx-serialization-compiler-plugin.backend"))
    testFixturesApi(project(":kotlinx-serialization-compiler-plugin.cli"))

    testFixturesApi("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.0")
    testFixturesApi("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")

    coreJsIrRuntimeForTests("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.0") { isTransitive = false }
    jsonJsIrRuntimeForTests("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0") { isTransitive = false }

    testRuntimeOnly(intellijCore())
    testRuntimeOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" {
        projectDefault()
    }
}

optInToExperimentalCompilerApi()

publish {
    artifactId = artifactId.replace("kotlinx-", "kotlin-")
}

val archiveName = "kotlin-serialization-compiler-plugin"
val archiveCompatName = "kotlinx-serialization-compiler-plugin"

val runtimeJar = runtimeJar {
    archiveBaseName.set(archiveName)
}

sourcesJar()
javadocJar()
testsJar()

val distCompat by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
}

val compatJar = tasks.register<Copy>("compatJar") {
    from(runtimeJar)
    into(layout.buildDirectory.dir("libsCompat"))
    rename {
        it.replace("kotlin-", "kotlinx-")
    }
}

artifacts {
    add(distCompat.name, layout.buildDirectory.dir("libsCompat").map { it.file("$archiveCompatName-$version.jar") }) {
        builtBy(runtimeJar, compatJar)
    }
}

compilerTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        dependsOn(":dist")
        workingDir = rootDir
        setUpJsIrBoxTests()
    }

    testGenerator("org.jetbrains.kotlinx.serialization.TestGeneratorKt")
}

fun Test.setUpJsIrBoxTests() {
    useJsIrBoxTests(version = version, buildDir = layout.buildDirectory)

    val localJsCoreRuntimeForTests: FileCollection = coreJsIrRuntimeForTests
    val localJsJsonRuntimeForTests: FileCollection = jsonJsIrRuntimeForTests

    doFirst {
        systemProperty("serialization.core.path", localJsCoreRuntimeForTests.asPath)
        systemProperty("serialization.json.path", localJsJsonRuntimeForTests.asPath)
    }
}

//region Workaround for KT-76495 and KTIJ-33877
val publications: PublicationContainer = extensions.getByType<PublishingExtension>().publications
val jpsCompatArtifactId = "kotlin-maven-serialization-for-jps-avoid-using-this"
val jpsCompatPublication = publications.register<MavenPublication>("jpsCompat") {
    artifactId = jpsCompatArtifactId
    from(components[ADHOC_COMPONENT_NAME])

    // Workaround for https://github.com/gradle/gradle/issues/12324
    (this as PublicationInternal<*>).isAlias = true
    configureKotlinPomAttributes(
        project,
        explicitDescription = "A workaround for KT-76495 and KTIJ-33877. Avoid depending on this artifact as it can be removed without prior notice."
    )
}
configureSbom(
    target = "${jpsCompatPublication.name.capitalize()}Publication",
    documentName = jpsCompatArtifactId,
    publication = jpsCompatPublication
)
//endregion
