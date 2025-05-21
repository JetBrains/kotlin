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
}

// WARNING: Native target is host-dependent. Re-running the same build on another host OS may bring to a different result.
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
        // WARNING: Native target is host-dependent. Re-running the same build on another host OS may bring to a different result.
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

    testApi(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":generators:test-generator"))
    testApi(project(":plugins:plugin-sandbox"))
    testApi(project(":compiler:incremental-compilation-impl"))
    testApi(projectTests(":compiler:incremental-compilation-impl"))

    testImplementation(projectTests(":js:js.tests"))
    testImplementation(libs.junit4)
    testApi(kotlinTest())

    // Dependencies for Kotlin/Native test infra:
    if (!kotlinBuildProperties.isInIdeaSync) {
        testImplementation(projectTests(":native:native.tests"))
    }
    testImplementation(project(":native:kotlin-native-utils"))
    testImplementation(commonDependency("org.jetbrains.teamcity:serviceMessages"))

    // todo: remove unnecessary dependencies
    testImplementation(project(":kotlin-compiler-runner-unshaded"))

    testImplementation(commonDependency("commons-lang:commons-lang"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":compiler:test-infrastructure"))
    testCompileOnly("org.jetbrains.kotlinx:atomicfu:0.25.0")

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testRuntimeOnly(kotlinStdlib())
    testRuntimeOnly(project(":kotlin-preloader")) // it's required for ant tests
    testRuntimeOnly(project(":compiler:backend-common"))
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
        generatedTestDir()
    }
}

testsJar()

projectTest(jUnitMode = JUnitMode.JUnit5) {
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

    doFirst {
        systemProperty("atomicfuJsIrRuntimeForTests.classpath", localAtomicfuJsIrRuntimeForTests.asPath)
        systemProperty("atomicfuJs.classpath", localAtomicfuJsClasspath.asPath)
        systemProperty("atomicfuJvm.classpath", localAtomicfuJvmClasspath.asPath)
    }
}

publish()
standardPublicJars()

val nativeTest = nativeTest(
    taskName = "nativeTest",
    tag = "atomicfu-native",
    requirePlatformLibs = true,
    customCompilerDependencies = listOf(atomicfuJvmClasspath),
    customTestDependencies = listOf(atomicfuNativeKlib),
    compilerPluginDependencies = listOf(atomicfuCompilerPluginForTests)
)

tasks.named("check") {
    // Depend on the test task that launches Native tests so that it will also run together with tests
    // for all other targets if K/N is enabled
    if (kotlinBuildProperties.isKotlinNativeEnabled) {
        dependsOn(nativeTest)
    }
}
