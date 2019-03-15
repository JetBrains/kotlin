
description = "Kotlin \"main\" script definition tests"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompile(project(":kotlin-main-kts"))
    testCompile(project(":kotlin-scripting-jvm-host"))
    testCompile(commonDep("junit"))
    compileOnly("org.apache.ivy:ivy:2.4.0") // for jps/pill
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

