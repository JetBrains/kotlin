import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.konan.target.HostManager

description = "Atomicfu Compiler Plugin"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("d8-configuration")
    id("test-inputs-check")
    id("java-test-fixtures")
}

// WARNING: Native target is host-dependent. Re-running the same build on another host OS may give a different result.
val nativeTargetName = HostManager.host.name

val antLauncherJar = configurations.create("antLauncherJar")
val testJsRuntime = configurations.create("testJsRuntime") {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_RUNTIME))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
    }
}

val atomicfuJsClasspath = configurations.create("atomicfuJsClasspath") {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
    }
}

val atomicfuJvmClasspath = configurations.create("atomicfuJvmClasspath")

val atomicfuNativeKlib = configurations.create("atomicfuNativeKlib") {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
        // WARNING: Native target is host-dependent. Re-running the same build on another host OS may give a different result.
        attribute(KotlinNativeTarget.konanTargetAttribute, nativeTargetName)
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
    }
}

val atomicfuJsIrRuntimeForTests = configurations.create("atomicfuJsIrRuntimeForTests") {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_RUNTIME))
    }
}

val atomicfuCompilerPluginForTests = configurations.create("atomicfuCompilerPluginForTests")

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
    compileOnly(project(":compiler:cli-base"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:ir.backend.common"))

    compileOnly(project(":compiler:backend.js"))

    compileOnly(project(":compiler:backend.jvm"))
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":native:native.config"))

    compileOnly(project(":core:descriptors"))
    compileOnly(project(":core:language.targets.jvm"))

    compileOnly(kotlinStdlib())

    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-compiler-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":generators:test-generator")))
    testFixturesApi(project(":plugins:plugin-sandbox"))
    testFixturesApi(project(":compiler:incremental-compilation-impl"))
    testFixturesApi(testFixtures(project(":compiler:incremental-compilation-impl")))

    testFixturesApi(testFixtures(project(":js:js.tests")))
    testFixturesApi(kotlinTest())

    // Dependencies for Kotlin/Native test infra:
    if (!kotlinBuildProperties.isInIdeaSync.get()) {
        testFixturesApi(testFixtures(project(":native:native.tests")))
    }
    testFixturesApi(project(":compiler:ir.backend.native"))
    testFixturesApi(project(":native:kotlin-native-utils"))
    testFixturesApi(testFixtures(project(":native:native.tests:klib-ir-inliner")))
    testFixturesApi(project(":kotlin-util-klib-abi"))
    testFixturesApi(commonDependency("org.jetbrains.teamcity:serviceMessages"))

    // todo: remove unnecessary dependencies
    testFixturesApi(project(":kotlin-compiler-runner-unshaded"))

    testFixturesApi(commonDependency("org.apache.commons:commons-lang3"))
    testFixturesCompileOnly("org.jetbrains.kotlinx:atomicfu:0.25.0")

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testImplementation(kotlinStdlib())
    testImplementation(testFixtures(project(":compiler:tests-compiler-utils")))
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

    testFixturesImplementation("org.jetbrains.kotlinx:atomicfu:0.25.0")
}

optInToExperimentalCompilerApi()
optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "testFixtures" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

optInToK1Deprecation()

projectTests {
    testTask {
        useJUnitPlatform {
            // Exclude all tests with the "atomicfu-native" tag. They should be launched by another test task.
            excludeTags("atomicfu-native")
        }
        useJsIrBoxTests(buildDir = layout.buildDirectory)

        addClasspathProperty(atomicfuJsIrRuntimeForTests, "atomicfuJsIrRuntimeForTests.classpath")
        addClasspathProperty(atomicfuJsClasspath, "atomicfuJs.classpath")
        addClasspathProperty(atomicfuJvmClasspath, "atomicfuJvm.classpath")
        addClasspathProperty(atomicfuCompilerPluginForTests, "atomicfu.compiler.plugin")

        // IncrementalK2JVMWithAtomicfuRunnerTestGenerated needs the compiler distribution.
        @OptIn(KotlinCompilerDistUsage::class)
        withDist()
    }

    nativeTestTask(
        taskName = "nativeTest",
        tag = "atomicfu-native",
        requirePlatformLibs = true,
        customCompilerDependencies = listOf(atomicfuJvmClasspath),
        customTestDependencies = listOf(atomicfuNativeKlib),
        compilerPluginDependencies = listOf(atomicfuCompilerPluginForTests)
    ) {
        addClasspathProperty(atomicfuNativeKlib, "atomicfuNative.classpath")

        // To workaround KTI-2421, we make these tests run on JDK 11 instead of the project-default JDK 8.
        // Kotlin test infra uses reflection to access JDK internals.
        // With JDK 11, some JVM args are required to silence the warnings caused by that:
        jvmArgs("--add-opens=java.base/java.io=ALL-UNNAMED")
    }

    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateAtomicfuTestsKt") {
        javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    }

    testData(project.isolated, "testData")
    // For test task only
    testData(project(":js:js.translator").isolated, "testData/_commonFiles")

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withTestJar()
    withJsRuntime()
    withMockJdkAnnotationsJar()
    withMockJdkRuntime()

    withMockJDKModifiedRuntime()
}

publish()
standardPublicJars()

tasks.named("check") {
    // Depend on the test task that launches Native tests so that it will also run together with tests
    // for all other targets if K/N is enabled
    if (kotlinBuildProperties.isKotlinNativeEnabled.get()) {
        dependsOn(tasks.named("nativeTest"))
    }
}
