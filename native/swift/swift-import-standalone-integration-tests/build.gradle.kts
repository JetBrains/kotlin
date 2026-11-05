plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
}

description = "Infrastructure for running Swift Import Standalone integration tests"

jvmToolchains {
    jdkVersion = JdkMajorVersion.JDK_25_0
    targetBytecodeVersion = JdkMajorVersion.JDK_25_0
}

dependencies {
    compileOnly(kotlinStdlib())

    testFixturesApi(project(":native:swift:swift-import-standalone"))
    testFixturesApi(project(":native:swift:kir-printer"))
    testFixturesApi(testFixtures(project(":native:native.tests")))
    testFixturesCompileOnly(project(":repo:test-runtime"))
}

sourceSets {
    "main" { none() }
    "testFixtures" { projectDefault() }
}

tasks.named("check") {
    dependsOn(subprojects.map { "${it.path}:check" })
}
