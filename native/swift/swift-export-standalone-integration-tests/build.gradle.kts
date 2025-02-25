plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Infrastructure for running Swift Export Standalone integration tests"

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":native:swift:swift-export-embeddable"))

    if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
        api(projectTests(":native:native.tests"))
    }
}

sourceSets {
    "main" { projectDefault() }
}
