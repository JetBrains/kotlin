import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.konan.target.HostManager

description = "Atomicfu Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("d8-configuration")
    id("project-tests-convention")
}

// WARNING: Native target is host-dependent. Re-running the same build on another host OS may give a different result.
val nativeTargetName = HostManager.host.name

val antLauncherJar by configurations.creating
val testJsRuntime by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_RUNTIME))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
    }
}

val atomicfuJsClasspath by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
    }
}

val atomicfuJvmClasspath by configurations.creating

val atomicfuNativeKlib by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
        // WARNING: Native target is host-dependent. Re-running the same build on another host OS may give a different result.
        attribute(KotlinNativeTarget.konanTargetAttribute, nativeTargetName)
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
    }
}

val atomicfuJsIrRuntimeForTests by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_RUNTIME))
    }
}

val atomicfuCompilerPluginForTests by configurations.creating

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)

    compileOnly(project(":compiler:fir:cones"))
    compileOnly(project(":compiler:fir:tree"))
    compileOnly(project(":compiler:fir:resolve"))
    compileOnly(project(":compiler:fir:plugin-utils"))
    compileOnly(project(":compiler:fir:checkers"))
    compileOnly(project(":compiler:fir:fir2ir"))
    compileOnly(project(":compiler:fir:entrypoint"))

    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:ir.backend.common"))

    compileOnly(project(":compiler:backend.js"))

    compileOnly(project(":compiler:backend.jvm"))
    compileOnly(project(":compiler:ir.tree"))

    compileOnly(kotlinStdlib())

    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(testFixtures(project(":compiler:test-infrastructure")))
    testImplementation(testFixtures(project(":compiler:test-infrastructure-utils")))
    testImplementation(testFixtures(project(":compiler:tests-compiler-utils")))
    testImplementation(testFixtures(project(":compiler:tests-common-new")))
    testImplementation(testFixtures(project(":generators:test-generator")))
    testImplementation(project(":plugins:plugin-sandbox"))
    testImplementation(project(":compiler:incremental-compilation-impl"))
    testImplementation(testFixtures(project(":compiler:incremental-compilation-impl")))

    testImplementation(testFixtures(project(":js:js.tests")))
    testImplementation(libs.junit4)
    testImplementation(kotlinTest())

    // Dependencies for Kotlin/Native test infra:
    if (!kotlinBuildProperties.isInIdeaSync) {
        testImplementation(testFixtures(project(":native:native.tests")))
    }
    testImplementation(project(":compiler:ir.backend.native"))
    testImplementation(project(":native:kotlin-native-utils"))
    testImplementation(testFixtures(project(":native:native.tests:klib-ir-inliner")))
    testImplementation(project(":kotlin-util-klib-abi"))
    testImplementation(commonDependency("org.jetbrains.teamcity:serviceMessages"))

    // todo: remove unnecessary dependencies
    testImplementation(project(":kotlin-compiler-runner-unshaded"))

    testImplementation(commonDependency("org.apache.commons:commons-lang3"))
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(testFixtures(project(":compiler:tests-common-new")))
    testImplementation(testFixtures(project(":compiler:test-infrastructure")))
    testCompileOnly("org.jetbrains.kotlinx:atomicfu:0.25.0")

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testRuntimeOnly(kotlinStdlib())
    testRuntimeOnly(project(":kotlin-preloader")) // it's required for ant tests
    testRuntimeOnly(commonDependency("org.fusesource.jansi", "jansi"))

    atomicfuJsClasspath("org.jetbrains.kotlinx:atomicfu-js:0.25.0") { isTransitive = false }
    atomicfuJsIrRuntimeForTests(project(":kotlinx-atomicfu-runtime"))  { isTransitive = false }
    atomicfuJvmClasspath("org.jetbrains.kotlinx:atomicfu:0.25.0") { isTransitive = false }
    atomicfuNativeKlib("org.jetbrains.kotlinx:atomicfu:0.25.0") { isTransitive = false }
    atomicfuCompilerPluginForTests(project(":kotlin-atomicfu-compiler-plugin"))
    // Implicit dependencies on native artifacts to run native tests on CI
    implicitDependencies("org.jetbrains.kotlinx:atomicfu-linuxx64:0.25.0") {
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        }
    }
    implicitDependencies("org.jetbrains.kotlinx:atomicfu-macosarm64:0.25.0"){
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        }
    }
    implicitDependencies("org.jetbrains.kotlinx:atomicfu-macosx64:0.25.0"){
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        }
    }
    implicitDependencies("org.jetbrains.kotlinx:atomicfu-iossimulatorarm64:0.25.0"){
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        }
    }
    implicitDependencies("org.jetbrains.kotlinx:atomicfu-mingwx64:0.25.0"){
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        }
    }

    embedded(project(":kotlinx-atomicfu-runtime")) {
        attributes {
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
            attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_RUNTIME))
        }
        isTransitive = false
    }

    testImplementation("org.jetbrains.kotlinx:atomicfu:0.25.0")

    testRuntimeOnly(libs.junit.vintage.engine)
}

optInToExperimentalCompilerApi()
optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        java.srcDirs("testFixtures")
        generatedTestDir()
    }
}

testsJar()

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        useJUnitPlatform {
            // Exclude all tests with the "atomicfu-native" tag. They should be launched by another test task.
            excludeTags("atomicfu-native")
        }
        useJsIrBoxTests(version = version, buildDir = layout.buildDirectory)

        workingDir = rootDir

        dependsOn(":dist")
        dependsOn(atomicfuJsIrRuntimeForTests)

        val localAtomicfuJsIrRuntimeForTests: FileCollection = atomicfuJsIrRuntimeForTests
        val localAtomicfuJsClasspath: FileCollection = atomicfuJsClasspath
        val localAtomicfuJvmClasspath: FileCollection = atomicfuJvmClasspath
        val localAtomicfuCompilerPluginClasspath: FileCollection = atomicfuCompilerPluginForTests

        doFirst {
            systemProperty("atomicfuJsIrRuntimeForTests.classpath", localAtomicfuJsIrRuntimeForTests.asPath)
            systemProperty("atomicfuJs.classpath", localAtomicfuJsClasspath.asPath)
            systemProperty("atomicfuJvm.classpath", localAtomicfuJvmClasspath.asPath)
            systemProperty("atomicfu.compiler.plugin", localAtomicfuCompilerPluginClasspath.asPath)
        }
    }

    nativeTestTask(
        taskName = "nativeTest",
        tag = "atomicfu-native",
        requirePlatformLibs = true,
        customCompilerDependencies = listOf(atomicfuJvmClasspath),
        customTestDependencies = listOf(atomicfuNativeKlib),
        compilerPluginDependencies = listOf(atomicfuCompilerPluginForTests)
    ) {
        val localAtomicfuNativeKlib: FileCollection = atomicfuNativeKlib
        doFirst {
            systemProperty("atomicfuNative.classpath", localAtomicfuNativeKlib.asPath)
        }

        // To workaround KTI-2421, we make these tests run on JDK 11 instead of the project-default JDK 8.
        // Kotlin test infra uses reflection to access JDK internals.
        // With JDK 11, some JVM args are required to silence the warnings caused by that:
        jvmArgs("--add-opens=java.base/java.io=ALL-UNNAMED")
    }

    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateAtomicfuTestsKt", doNotSetFixturesSourceSetDependency = true) {
        javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    }

    withJvmStdlibAndReflect()
}

publish()
standardPublicJars()

tasks.named("check") {
    // Depend on the test task that launches Native tests so that it will also run together with tests
    // for all other targets if K/N is enabled
    if (kotlinBuildProperties.isKotlinNativeEnabled) {
        dependsOn(tasks.named("nativeTest"))
    }
}


