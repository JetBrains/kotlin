import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
}

dependencies {
//    testImplementation(kotlinStdlib())
//    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
//    testImplementation(intellijCore())
//    testImplementation(commonDependency("commons-lang:commons-lang"))
//    testImplementation(commonDependency("org.jetbrains.teamcity:serviceMessages"))
//    testImplementation(project(":kotlin-compiler-runner-unshaded"))
//    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:tests-common-new"))
//    testImplementation(projectTests(":compiler:test-infrastructure"))
    testImplementation(projectTests(":generators:test-generator"))
    testImplementation(project(":native:kotlin-native-utils"))
//    testImplementation(project(":native:executors"))
    testApiJUnit5()
//    testImplementation(commonDependency("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }
//
//    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps:trove4j"))
//    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))
}

val generationRoot = projectDir.resolve("tests-gen")

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        java.srcDirs(generationRoot.name)
    }
}

val testTags = findProperty("kotlin.native.tests.tags")?.toString()
// Note: arbitrary JUnit tag expressions can be used in this property.
// See https://junit.org/junit5/docs/current/user-guide/#running-tests-tag-expressions
val test by nativeTest("test", testTags).apply {
    configure {
        dependsOn(":dist")
    }
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateNativeDiagnosticTestsKt") {
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    dependsOn(":compiler:generateTestData")
}
