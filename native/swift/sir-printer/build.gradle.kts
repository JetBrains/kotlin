plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("project-tests-convention")
}

description = "Printer for SIR"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":native:swift:sir"))

    implementation(project(":core:util.runtime"))

    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.api)

    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(testFixtures(project(":compiler:tests-common-new")))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val testDataDir = projectDir.resolve("testData")

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        inputs.dir(testDataDir)
    }
}

testsJar()

publish()

runtimeJar()
sourcesJar()
javadocJar()
