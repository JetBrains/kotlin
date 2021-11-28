
description = "Kotlin \"main\" script definition tests"

plugins {
    kotlin("jvm")
}

dependencies {
    testApi(project(":kotlin-main-kts"))
    testCompileOnly(project(":compiler:cli"))
    testCompileOnly(project(":kotlin-scripting-jvm-host-unshaded"))
    testApi(kotlinStdlib("jdk8"))
    testApi(commonDep("junit"))
    testApi(projectTests(":kotlin-scripting-compiler")) { isTransitive = false }
    testImplementation(project(":kotlin-compiler-embeddable"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
}

projectTest(taskName = "testWithIr", parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
    systemProperty("kotlin.script.test.base.compiler.arguments", "-Xuse-ir")
}
