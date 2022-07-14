plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
}

dependencies {
    testImplementation(kotlinStdlib())
    testApi(projectTests(":compiler:cli"))
    testApi(projectTests(":compiler:incremental-compilation-impl"))
    testApi(projectTests(":plugins:jvm-abi-gen"))
    testApi(projectTests(":generators:test-generator"))
    testApi(testFixtures(project(":kotlin-project-model")))
    testApi(project(":kotlin-project-model"))
    testCompileOnly(project(":kotlin-reflect-api"))
    testImplementation(project(":kotlin-reflect"))
    testImplementation(projectTests(":compiler:test-infrastructure-utils"))
    testImplementation(projectTests(":compiler:test-infrastructure"))
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":js:js.tests"))
    testApiJUnit5()

    // Refs to abstract supertypes for generated tests
    testApi(projectTests(":kotlin-gradle-plugin-integration-tests"))

    if (Ide.IJ()) {
        testCompileOnly(jpsBuildTest())
        testApi(jpsBuildTest())
    }
}

val generateKpmTests by generator("org.jetbrains.kotlin.kpm.GenerateKpmTestsKt") {
}
