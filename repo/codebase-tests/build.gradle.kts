plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testImplementation(intellijCore())
    testImplementation(projectTests(":compiler:tests-common"))

    testImplementation(libs.jackson.dataformat.xml)
    testImplementation(libs.jackson.module.kotlin)
    testImplementation("com.fasterxml.woodstox:woodstox-core:6.5.1")
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit4)

    testImplementation("org.eclipse.jgit:org.eclipse.jgit:5.13.0.202109080827-r")
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