plugins {
    kotlin("jvm")
}

description = "Common part of Swift and Objective-C exports."

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":analysis:analysis-api"))
    api(project(":core:compiler.common"))
    api(project(":compiler:psi:psi-api"))
}

kotlin {
    explicitApi()
}

sourceSets {
    "main" { projectDefault() }
}
