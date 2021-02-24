
description = "Kotlin \"main\" script definition tests"

plugins {
    kotlin("jvm")
}

dependencies {
    testCompile(project(":kotlin-main-kts"))
    testCompileOnly(project(":compiler:cli"))
    testCompileOnly(project(":kotlin-scripting-jvm-host-unshaded"))
    testCompile(kotlinStdlib("jdk8"))
    testCompile(commonDep("junit"))
    testCompile(projectTests(":kotlin-scripting-compiler")) { isTransitive = false }
    testRuntime(project(":kotlin-compiler-embeddable"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
}
