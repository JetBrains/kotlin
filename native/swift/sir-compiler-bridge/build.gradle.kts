plugins {
    kotlin("jvm")
}

description = "SIR to Kotlin bindings generator"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":native:swift:sir"))

    testApi(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.api)

    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:tests-common-new"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

val testDataDir = projectDir.resolve("testData")

projectTest(jUnitMode = JUnitMode.JUnit5) {
    inputs.dir(testDataDir)
    workingDir = rootDir
    useJUnitPlatform { }
}

testsJar()