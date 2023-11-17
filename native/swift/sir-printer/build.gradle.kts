plugins {
    kotlin("jvm")
}

description = "Printer for SIR"

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
    "test" { projectDefault() }
}