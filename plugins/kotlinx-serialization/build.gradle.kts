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
    id("d8-configuration")
    id("java-test-fixtures")
    id("project-tests-convention")
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

// WARNING: Native target is host-dependent. Re-running the same build on another host OS may give a different result.
val nativeTargetName = HostManager.host.name

val coreNativeRuntimeForTests by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
        // WARNING: Native target is host-dependent. Re-running the same build on another host OS may give a different result.
        attribute(KotlinNativeTarget.konanTargetAttribute, nativeTargetName)
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
    }
}

val jsonNativeRuntimeForTests by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
        // WARNING: Native target is host-dependent. Re-running the same build on another host OS may give a different result.
        attribute(KotlinNativeTarget.konanTargetAttribute, nativeTargetName)
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
    }
}

val serializationPluginForTests by configurations.creating

fun DependencyHandlerScope.implicitKotlinApiDependency(notation: Any) {
    implicitDependencies(notation) {
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        }
    }
}

dependencies {
    embedded(project(":kotlinx-serialization-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlinx-serialization-compiler-plugin.k1")) { isTransitive = false }
    embedded(project(":kotlinx-serialization-compiler-plugin.k2")) { isTransitive = false }
    embedded(project(":kotlinx-serialization-compiler-plugin.backend")) { isTransitive = false }
    embedded(project(":kotlinx-serialization-compiler-plugin.cli")) { isTransitive = false }

    testFixturesApi(project(":kotlinx-serialization-compiler-plugin.cli"))

    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-fir")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-impl-base")))
    testFixturesApi(testFixtures(project(":analysis:low-level-api-fir:low-level-api-fir-compiler-tests")))

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

    coreJsIrRuntimeForTests("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3") { isTransitive = false }
    jsonJsIrRuntimeForTests("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3") { isTransitive = false }
    coreNativeRuntimeForTests("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3") { isTransitive = false }
    jsonNativeRuntimeForTests("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3") { isTransitive = false }
    serializationPluginForTests(project(":kotlinx-serialization-compiler-plugin"))

    testRuntimeOnly(intellijCore())

    // Dependencies for Kotlin/Native test infra:
    testFixturesApi(testFixtures(project(":native:native.tests")))
    testFixturesApi(testFixtures(project(":native:native.tests:klib-ir-inliner")))

    // Implicit dependencies on CORE and JSON native artifacts to run native tests on CI
    listOf(
        "linuxx64",
        "macosarm64",
        "macosx64",
        "iossimulatorarm64",
        "mingwx64"
    ).forEach {
        implicitKotlinApiDependency("org.jetbrains.kotlinx:kotlinx-serialization-core-$it:1.7.3")
        implicitKotlinApiDependency("org.jetbrains.kotlinx:kotlinx-serialization-json-$it:1.7.3")
    }
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

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5, defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_11_0)) {
        useJUnitPlatform {
            // Exclude all tests with the "serialization-native" tag. They should be launched by another test task.
            excludeTags("serialization-native")
        }

        dependsOn(":dist")
        workingDir = rootDir
        setUpJsIrBoxTests()
    }

    nativeTestTask(
        taskName = "nativeTest",
        tag = "serialization-native", // Include all tests with the "serialization-native" tag
        requirePlatformLibs = false,
        customTestDependencies = listOf(coreNativeRuntimeForTests, jsonNativeRuntimeForTests),
        compilerPluginDependencies = listOf(serializationPluginForTests)
    )

    testGenerator("org.jetbrains.kotlinx.serialization.GenerateSerializationTestsKt")

    withJvmStdlibAndReflect()
}

fun Test.setUpJsIrBoxTests() {
    useJsIrBoxTests(buildDir = layout.buildDirectory)

    jvmArgumentProviders.add(objects.newInstance<SystemPropertyClasspathProvider>().apply {
        classpath.from(coreJsIrRuntimeForTests)
        property.set("serialization.core.path")
    })
    jvmArgumentProviders.add(objects.newInstance<SystemPropertyClasspathProvider>().apply {
        classpath.from(jsonJsIrRuntimeForTests)
        property.set("serialization.json.path")
    })
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
