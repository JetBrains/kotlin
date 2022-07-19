plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
}

dependencies {
    testImplementation(kotlinStdlib())
    testApi(projectTests(":compiler:cli"))
    testApi(projectTests(":generators:test-generator"))
    testApi(testFixtures(project(":kotlin-project-model")))
    testApi(project(":kotlin-project-model"))
    testApi(projectTests(":kotlin-gradle-plugin-integration-tests"))
    testCompileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testImplementation(projectTests(":compiler:test-infrastructure-utils"))
    testImplementation(projectTests(":compiler:test-infrastructure"))
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":js:js.tests"))
    testApiJUnit5()

    if (Ide.IJ()) {
        testCompileOnly(jpsBuildTest())
        testApi(jpsBuildTest())
    }
}

val generateKpmTests by generator("org.jetbrains.kotlin.kpm.GenerateKpmTestsKt") {
}
