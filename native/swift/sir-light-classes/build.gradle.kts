plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Implementation of SwiftIR backed by Analysis API"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":native:swift:sir"))
    api(project(":native:swift:sir-providers"))
    compileOnly(project(":native:analysis-api-based-export-common"))

    compileOnly(project(":compiler:psi"))
    compileOnly(project(":analysis:analysis-api"))

    testApi(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(project(":native:analysis-api-based-test-utils"))
    testImplementation(project(":compiler:tests-common", "tests-jar"))
}

nativeTest("test", null)

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()

publish()

runtimeJar()
sourcesJar()
javadocJar()