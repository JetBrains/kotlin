
description = "Kotlin \"main\" script definition tests"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompile(project(":kotlin-main-kts"))
    testCompile(project(":kotlin-scripting-jvm-host"))
    testCompile(commonDep("junit"))
    testRuntime("org.apache.ivy:ivy:2.4.0")
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

