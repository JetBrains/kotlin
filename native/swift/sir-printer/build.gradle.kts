plugins {
    kotlin("jvm")
}

description = "Printer for SIR"

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":native:swift:sir"))

    implementation(project(":core:util.runtime"))

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