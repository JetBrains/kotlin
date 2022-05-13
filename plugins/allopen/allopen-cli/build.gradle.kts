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

    testApi(intellijCore())

    testApiJUnit5()
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

runtimeJar()

sourcesJar()

javadocJar()

testsJar()

projectTest(parallel = true) {
    workingDir = rootDir
    useJUnitPlatform()
}
