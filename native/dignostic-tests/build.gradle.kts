plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":generators:test-generator"))
    testImplementation(project(":native:kotlin-native-utils"))
    testApiJUnit5()
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
