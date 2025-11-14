import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("project-tests-convention")
}

description = "Implementation of SwiftIR backed by Analysis API"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":native:swift:sir"))
    api(project(":native:swift:sir-providers"))
    implementation(project(":native:analysis-api-based-export-common"))

    api(project(":compiler:psi:psi-api"))
    api(project(":analysis:analysis-api"))

    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(kotlinTest())
    testImplementation(project(":native:analysis-api-based-test-utils"))
    testImplementation(testFixtures(project(":compiler:tests-common")))
}

projectTests {
    nativeTestTask("test") {
        dependsOn(":kotlin-native:distInvalidateStaleCaches")
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()

publish()

runtimeJar()
sourcesJar()
javadocJar()

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.optIn.addAll(
        "org.jetbrains.kotlin.analysis.api.KaContextParameterApi",
        "org.jetbrains.kotlin.analysis.api.KaExperimentalApi",
    )
}
