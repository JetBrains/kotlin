
description = "Kotlin AllOpen Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(intellijCore())

    runtimeOnly(kotlinStdlib())

    testApi(project(":compiler:backend"))
    testApi(project(":compiler:cli"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(commonDependency("junit:junit"))

    testApi(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()

sourcesJar()

javadocJar()

testsJar()

projectTest(parallel = true) {
    workingDir = rootDir
}
