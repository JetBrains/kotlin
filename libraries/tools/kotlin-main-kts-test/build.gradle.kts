
description = "Kotlin \"main\" script definition tests"

plugins {
    kotlin("jvm")
}

dependencies {
    testCompile(project(":kotlin-main-kts"))
    testCompileOnly(project(":compiler:cli"))
    testCompileOnly(project(":kotlin-scripting-jvm-host-unshaded"))
    testCompile(commonDep("junit"))
    testCompile(projectTests(":kotlin-scripting-compiler-unshaded")) { isTransitive = false }
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
}
