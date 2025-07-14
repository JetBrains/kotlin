import org.gradle.api.publish.internal.PublicationInternal
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.konan.target.HostManager
import plugins.KotlinBuildPublishingPlugin.Companion.ADHOC_COMPONENT_NAME
import plugins.configureKotlinPomAttributes

description = "Kotlin Serialization Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("d8-configuration")
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

// WARNING: Native target is host-dependent. Re-running the same build on another host OS may bring to a different result.
val nativeTargetName = HostManager.host.name

val coreNativeRuntimeForTests by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
        // WARNING: Native target is host-dependent. Re-running the same build on another host OS may bring to a different result.
        attribute(KotlinNativeTarget.konanTargetAttribute, nativeTargetName)
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
    }
}

val jsonNativeRuntimeForTests by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
        // WARNING: Native target is host-dependent. Re-running the same build on another host OS may bring to a different result.
        attribute(KotlinNativeTarget.konanTargetAttribute, nativeTargetName)
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
    }
}

val serializationPluginForTests by configurations.creating

dependencies {
    embedded(project(":kotlinx-serialization-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlinx-serialization-compiler-plugin.k1")) { isTransitive = false }
    embedded(project(":kotlinx-serialization-compiler-plugin.k2")) { isTransitive = false }
    embedded(project(":kotlinx-serialization-compiler-plugin.backend")) { isTransitive = false }
    embedded(project(":kotlinx-serialization-compiler-plugin.cli")) { isTransitive = false }

    testApi(project(":compiler:backend"))
    testApi(project(":compiler:cli"))
    testApi(project(":kotlinx-serialization-compiler-plugin.cli"))

    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(project(":compiler:fir:plugin-utils"))
    testImplementation(projectTests(":generators:test-generator"))
    testImplementation(projectTests(":js:js.tests"))
    testImplementation(projectTests(":analysis:analysis-api-fir"))
    testImplementation(projectTests(":analysis:analysis-api-impl-base"))
    testImplementation(projectTests(":analysis:low-level-api-fir"))

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testApi(project(":kotlinx-serialization-compiler-plugin.common"))
    testApi(project(":kotlinx-serialization-compiler-plugin.k1"))
    testApi(project(":kotlinx-serialization-compiler-plugin.k2"))
    testApi(project(":kotlinx-serialization-compiler-plugin.backend"))
    testApi(project(":kotlinx-serialization-compiler-plugin.cli"))

    testApi("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.0")
    testApi("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")

    coreJsIrRuntimeForTests("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.0") { isTransitive = false }
    jsonJsIrRuntimeForTests("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0") { isTransitive = false }
    coreNativeRuntimeForTests("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.0") { isTransitive = false }
    jsonNativeRuntimeForTests("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0") { isTransitive = false }
    serializationPluginForTests(project(":kotlinx-serialization-compiler-plugin"))

    testRuntimeOnly(intellijCore())
    testRuntimeOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))

    // Dependencies for Kotlin/Native test infra:
    if (!kotlinBuildProperties.isInIdeaSync) {
        testImplementation(projectTests(":native:native.tests"))
    }
    testImplementation(project(":native:kotlin-native-utils"))
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
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

projectTest(parallel = true, jUnitMode = JUnitMode.JUnit5) {
    useJUnitPlatform {
        // Exclude all tests with the "serialization-native" tag. They should be launched by another test task.
        excludeTags("serialization-native")
    }

    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
    setUpJsIrBoxTests()
}

val nativeTest = nativeTest(
    taskName = "nativeTest",
    tag = "serialization-native", // Include all tests with the "serialization-native" tag
    requirePlatformLibs = false,
    customTestDependencies = listOf(coreNativeRuntimeForTests, jsonNativeRuntimeForTests),
    compilerPluginDependencies = listOf(serializationPluginForTests)
)

val generateTests by generator("org.jetbrains.kotlinx.serialization.TestGeneratorKt")

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