plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Infrastructure for running Swift Export Standalone integration tests"

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":native:swift:swift-export-standalone"))
    implementation(project(":native:external-projects-test-utils"))

    if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
        api(testFixtures(project(":native:native.tests")))
    }
}

sourceSets {
    "main" { projectDefault() }
}
