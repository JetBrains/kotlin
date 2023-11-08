plugins {
    kotlin("jvm")
}

description = "Infrastructure of transformations over SIR"

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":native:swift:sir"))

    testImplementation(libs.junit4)
    testImplementation(projectTests(":compiler:tests-common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}