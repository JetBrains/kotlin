plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Common part of Swift and Objective-C exports."

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":analysis:analysis-api"))
    api(project(":core:compiler.common"))
    api(project(":compiler:psi"))
}

kotlin {
    explicitApi()
}

sourceSets {
    "main" { projectDefault() }
}