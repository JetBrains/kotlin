import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
}

description = "Standalone Runner for Swift Export"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    implementation(project(":native:swift:sir"))
    implementation(project(":native:swift:sir-providers"))
    implementation(project(":native:swift:sir-light-classes"))
    implementation(project(":native:swift:sir-printer"))

    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-api-standalone"))

    implementation(project(":native:analysis-api-klib-reader"))

    testApi(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.api)

    testRuntimeOnly(testFixtures(project(":analysis:low-level-api-fir")))
    testRuntimeOnly(testFixtures(project(":analysis:analysis-api-impl-base")))
    testImplementation(testFixtures(project(":analysis:analysis-api-fir")))
    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(testFixtures(project(":compiler:tests-common-new")))

    if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
        testApi(testFixtures(project(":native:native.tests")))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

val testTags = findProperty("kotlin.native.tests.tags")?.toString()
val test by nativeTest("test", testTags) {
    dependsOn(":kotlin-native:distInvalidateStaleCaches")
}

publish()

runtimeJar()
sourcesJar()
javadocJar()

testsJar()
