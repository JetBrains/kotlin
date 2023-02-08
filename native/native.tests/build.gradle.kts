import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_11_0)

dependencies {
    testImplementation(kotlinStdlib())
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testImplementation(intellijCore())
    testImplementation(commonDependency("commons-lang:commons-lang"))
    testImplementation(commonDependency("org.jetbrains.teamcity:serviceMessages"))
    testImplementation(project(":kotlin-compiler-runner-unshaded"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":compiler:test-infrastructure"))
    testImplementation(projectTests(":generators:test-generator"))
    testApiJUnit5()

    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps:trove4j"))
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))
}

val generationRoot = projectDir.resolve("tests-gen")

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        java.srcDirs(generationRoot.name)
    }
}

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(plugin = "idea")
    idea {
        module.generatedSourceDirs.addAll(listOf(generationRoot))
    }
}

// Tasks that run different sorts of tests. Most frequent use case: running specific tests at TeamCity.
val infrastructureTest = nativeTest("infrastructureTest", "infrastructure")
val codegenBoxTest = nativeTest("codegenBoxTest", "codegen")
val codegenK2BoxTest = nativeTest("codegenK2BoxTest", "codegenK2")
val stdlibTest = nativeTest("stdlibTest", "stdlib")
val kotlinTestLibraryTest = nativeTest("kotlinTestLibraryTest", "kotlin-test")
val klibAbiTest = nativeTest("klibAbiTest", "klib-abi")
val klibBinaryCompatibilityTest = nativeTest("klibBinaryCompatibilityTest", "klib-binary-compatibility")
val cinteropTest = nativeTest("cinteropTest", "cinterop")
val debuggerTest = nativeTest("debuggerTest", "debugger")
val cachesTest = nativeTest("cachesTest", "caches")
val k1libContentsTest = nativeTest("k1libContentsTest", "k1libContents")
val k2libContentsTest = nativeTest("k2libContentsTest", "k2libContents")

// "test" task is created by convention. We can't just remove it. Let's enable it in developer's environment, so it can be used
// to run any test from IDE or from console, but disable it at TeamCity where it is not supposed to be ever used.
val test by nativeTest("test", null /* no tags */).apply {
    if (kotlinBuildProperties.isTeamcityBuild) {
        configure { doFirst { throw GradleException("Task $path is not supposed to be executed in TeamCity environment") } }
    }
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateNativeTestsKt") {
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    dependsOn(":compiler:generateTestData")
}
