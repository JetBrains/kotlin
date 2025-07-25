plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("compiler-tests-convention")
}

description = "Printer for SIR"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":native:swift:sir"))

    implementation(project(":core:util.runtime"))

    testApi(platform(libs.junit.bom))
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

compilerTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        inputs.dir(testDataDir)
    }
}

testsJar()

publish()

runtimeJar()
sourcesJar()
javadocJar()
