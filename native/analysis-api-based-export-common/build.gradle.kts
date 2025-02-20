plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Common part of Swift and Objective-C exports."

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":analysis:analysis-api"))
}

sourceSets {
    "main" { projectDefault() }
}