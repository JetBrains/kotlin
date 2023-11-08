plugins {
    kotlin("jvm")
}

description = "Swift Intermediate Representation"

dependencies {
    compileOnly(kotlinStdlib())

    testImplementation(libs.junit4)
    testImplementation(projectTests(":compiler:tests-common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}