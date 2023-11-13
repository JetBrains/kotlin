plugins {
    kotlin("jvm")
}

description = "Swift Intermediate Representation"

dependencies {
    compileOnly(kotlinStdlib())

    testApi(platform(libs.junit.bom))
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