plugins {
    kotlin("jvm")
}

description = "Build Swift IR from Analysis"

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":native:swift:sir"))
    api(project(":analysis:analysis-api"))

    testImplementation(libs.junit4)
    testImplementation(projectTests(":compiler:tests-common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}