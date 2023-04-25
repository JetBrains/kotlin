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
    testImplementation(project(":native:kotlin-native-utils"))
    testImplementation(project(":native:executors"))
    testApiJUnit5()
    testImplementation(commonDependency("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }

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
val stdlibK2Test = nativeTest("stdlibK2Test", "stdlibK2")
val kotlinTestLibraryTest = nativeTest("kotlinTestLibraryTest", "kotlin-test")
val kotlinTestK2LibraryTest = nativeTest("kotlinTestK2LibraryTest", "kotlin-testK2")
val partialLinkageTest = nativeTest("partialLinkageTest", "partial-linkage")
val cinteropTest = nativeTest("cinteropTest", "cinterop")
val debuggerTest = nativeTest("debuggerTest", "debugger")
val cachesTest = nativeTest("cachesTest", "caches")
val k1libContentsTest = nativeTest("k1libContentsTest", "k1libContents")
val k2libContentsTest = nativeTest("k2libContentsTest", "k2libContents")

val testTags = findProperty("kotlin.native.tests.tags")?.toString()
// Note: arbitrary JUnit tag expressions can be used in this property.
// See https://junit.org/junit5/docs/current/user-guide/#running-tests-tag-expressions
val test by nativeTest("test", testTags)

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateNativeTestsKt") {
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    dependsOn(":compiler:generateTestData")
}
