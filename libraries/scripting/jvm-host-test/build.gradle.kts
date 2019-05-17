
plugins {
    kotlin("jvm")
}

jvmTarget = "1.6"

dependencies {
    testCompile(intellijCoreDep())
    testCompile(intellijDep()) { includeJars("openapi", "idea", "log4j") }
    testCompile(project(":kotlin-scripting-jvm-host"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit"))
    testCompile(project(":compiler:daemon-common")) // TODO: fix import (workaround for jps build)
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    workingDir = rootDir
}
