plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
}

description = "Infrastructure for running Swift Export Standalone integration tests"

dependencies {
    compileOnly(kotlinStdlib())

    testFixturesApi(project(":native:swift:swift-export-standalone"))
    testFixturesImplementation(project(":native:external-projects-test-utils"))
    testFixturesImplementation(project(":kotlin-util-klib-metadata"))
    testFixturesApi(testFixtures(project(":native:native.tests")))
    testFixturesCompileOnly(testFederationRuntime)
}

sourceSets {
    "main" { none() }
    "testFixtures" { projectDefault() }
}

tasks.named("check") {
    dependsOn(subprojects.map { "${it.path}:check" })
}
