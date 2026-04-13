plugins {
    kotlin("jvm")
    id("project-tests-convention")
    id("test-inputs-check")
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

projectTests {
    testData(isolated, "testData")

    testTask(jUnitMode = JUnitMode.JUnit5)
}

testsJar()

publish()

runtimeJar()
sourcesJar()
javadocJar()
