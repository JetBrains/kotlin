
description = "Kotlin \"main\" script definition tests"

plugins {
    kotlin("jvm")
}

dependencies {
    testCompile(project(":kotlin-main-kts"))
    testCompileOnly(project(":compiler:cli"))
    testCompile(project(":kotlin-scripting-jvm-host-embeddable"))
    testCompile(commonDep("junit"))
    testCompile(projectTests(":kotlin-scripting-compiler")) { isTransitive = false }
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
}
