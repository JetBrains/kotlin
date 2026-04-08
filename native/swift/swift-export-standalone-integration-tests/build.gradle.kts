plugins {
    kotlin("jvm")
}

description = "Infrastructure for running Swift Export Standalone integration tests"

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":native:swift:swift-export-standalone"))
    implementation(project(":native:external-projects-test-utils"))
    api(testFixtures(project(":native:native.tests")))
    compileOnly(testFederationRuntime)
}

sourceSets {
    "main" { projectDefault() }
}
