import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

// WARNING: Native target is host-dependent. Re-running the same build on another host OS may bring to a different result.
val nativeTargetName = HostManager.host.name

val litmusktCoreNativeKlib by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
        // WARNING: Native target is host-dependent. Re-running the same build on another host OS may bring to a different result.
        attribute(KotlinNativeTarget.konanTargetAttribute, nativeTargetName)
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
    }
}

val litmusktTestsuiteNativeKlib by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
        // WARNING: Native target is host-dependent. Re-running the same build on another host OS may bring to a different result.
        attribute(KotlinNativeTarget.konanTargetAttribute, nativeTargetName)
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))

    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:backend.jvm"))
    compileOnly(project(":compiler:ir.tree"))

    compileOnly(kotlinStdlib())

    testApi(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":generators:test-generator"))
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

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testRuntimeOnly(kotlinStdlib())
    testRuntimeOnly(project(":compiler:backend-common"))

    // use local subprojects as dependencies 
    litmusktCoreNativeKlib(project(":litmuskt:core")) { isTransitive = false }
    litmusktTestsuiteNativeKlib(project(":litmuskt:testsuite")) { isTransitive = false }

// TODO: should be useful in the future
// Implicit dependencies on native artifacts to run native tests on CI
//    implicitDependencies("org.jetbrains.kotlinx:atomicfu-linuxx64:0.21.0") {
//        attributes {
//            attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
//        }
//    }
//    implicitDependencies("org.jetbrains.kotlinx:atomicfu-macosarm64:0.21.0"){
//        attributes {
//            attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
//        }
//    }
//    implicitDependencies("org.jetbrains.kotlinx:atomicfu-macosx64:0.21.0"){
//        attributes {
//            attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
//        }
//    }
//    implicitDependencies("org.jetbrains.kotlinx:atomicfu-mingwx64:0.21.0"){
//        attributes {
//            attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
//        }
//    }
}

//optInToExperimentalCompilerApi()
//optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()
useD8Plugin()

projectTest(jUnitMode = JUnitMode.JUnit5) {
    useJUnitPlatform {
        // Exclude all tests with the "litmuskt-native" tag. They should be launched by another test task.
        excludeTags("litmuskt-native")
    }
    workingDir = rootDir
    dependsOn(":dist")
}

publish()
standardPublicJars()

val nativeTest = nativeTest(
    taskName = "nativeTest",
    tag = "litmuskt-native", // Include all tests with the "litmuskt-native" tag.
    requirePlatformLibs = true,
    customTestDependencies = listOf(litmusktCoreNativeKlib, litmusktTestsuiteNativeKlib)
)

tasks.named("check") {
    // Depend on the test task that launches Native tests so that it will also run together with tests
    // for all other targets if K/N is enabled
    if (kotlinBuildProperties.isKotlinNativeEnabled) {
        dependsOn(nativeTest)
    }
}
