
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "High-level Swift export API for Kotlin Playground"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    implementation(project(":native:swift:sir"))
    implementation(project(":native:swift:sir-light-classes"))
    implementation(project(":native:swift:sir-printer"))
    implementation(project(":native:swift:sir-providers"))

    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-api-standalone"))

    testApi(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.api)

    testRuntimeOnly(projectTests(":analysis:low-level-api-fir"))
    testRuntimeOnly(projectTests(":analysis:analysis-api-impl-base"))
    testImplementation(projectTests(":analysis:analysis-api-fir"))
    testImplementation(projectTests(":analysis:analysis-test-framework"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:tests-common-new"))

    if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
        testApi(projectTests(":native:native.tests"))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

val testDataDir = projectDir.resolve("testData")

val test by nativeTest("test", null) {
    inputs.dir(testDataDir)
    workingDir = rootDir
    useJUnitPlatform { }
}

testsJar()

if (kotlinBuildProperties.isSwiftExportPluginPublishingEnabled) {
    publish()
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())