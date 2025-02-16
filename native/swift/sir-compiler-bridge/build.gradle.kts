plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "SIR to Kotlin bindings generator"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":native:swift:sir"))
    // SIR providers give access to Kotlin origin of SIR declaration.
    implementation(project(":native:swift:sir-providers"))
    // Allow direct access to Analysis API in bridges to avoid modeling Kotlin code all over again.
    implementation(project(":analysis:analysis-api"))
    // Analysis API depends on some API from the compiler core (like FqName).
    implementation(project(":core:compiler.common"))

    testApi(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.api)

    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(project(":native:analysis-api-based-test-utils"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

nativeTest("test", null)

testsJar()

publish()

runtimeJar()
sourcesJar()
javadocJar()