plugins {
    kotlin("jvm")
}

description = "Infrastructure of transformations over SIR"

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":native:swift:sir"))

    testImplementation(kotlin("test-junit5"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(projectTests(":compiler:tests-common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    workingDir = rootDir
    useJUnitPlatform { }
}