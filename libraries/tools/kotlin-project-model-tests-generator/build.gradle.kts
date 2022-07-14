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
    testApi(projectTests(":plugins:android-extensions-compiler"))
    testApi(projectTests(":plugins:parcelize:parcelize-compiler"))
    testApi(projectTests(":kotlin-annotation-processing"))
    testApi(projectTests(":kotlin-annotation-processing-cli"))
    testApi(projectTests(":kotlin-allopen-compiler-plugin"))
    testApi(projectTests(":kotlin-noarg-compiler-plugin"))
    testApi(projectTests(":kotlin-lombok-compiler-plugin"))
    testApi(projectTests(":kotlin-sam-with-receiver-compiler-plugin"))
    testApi(projectTests(":kotlinx-serialization-compiler-plugin"))
    testApi(projectTests(":kotlinx-atomicfu-compiler-plugin"))
    testApi(projectTests(":plugins:fir-plugin-prototype"))
    testApi(projectTests(":plugins:fir-plugin-prototype:fir-plugin-ic-test"))
    testApi(projectTests(":generators:test-generator"))
    testApi(testFixtures(project(":kotlin-project-model")))
    testApi(project(":kotlin-project-model"))
    testApi(projectTests(":kotlin-gradle-plugin-integration-tests"))
    testCompileOnly(project(":kotlin-reflect-api"))
    testImplementation(project(":kotlin-reflect"))
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
