plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testImplementation(intellijCore())
    testImplementation(projectTests(":compiler:tests-common"))

    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.12.7")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.7")
    testImplementation("com.fasterxml.woodstox:woodstox-core:6.2.4")
}

sourceSets {
    "main" {}
    "test" {
        projectDefault()
    }
}

projectTest() {
    dependsOn(":dist")
    workingDir = rootDir
}

testsJar()