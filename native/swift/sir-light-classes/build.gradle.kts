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

    api(project(":compiler:psi"))
    api(project(":analysis:analysis-api"))

    testApi(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(project(":native:analysis-api-based-test-utils"))

    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.jna:jna"))
    testRuntimeOnly(intellijJDom())
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps:trove4j"))
    testRuntimeOnly(libs.intellij.fastutil)

    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))

    testImplementation(projectTests(":analysis:analysis-api-impl-base"))
    testImplementation(projectTests(":analysis:analysis-test-framework"))
    testRuntimeOnly(projectTests(":analysis:low-level-api-fir"))
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