description = "Kotlin Power-Assert Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val junit5Classpath by configurations.creating

dependencies {
    embedded(project(":kotlin-power-assert-compiler-plugin.backend")) { isTransitive = false }
    embedded(project(":kotlin-power-assert-compiler-plugin.cli")) { isTransitive = false }

    testImplementation(project(":kotlin-power-assert-compiler-plugin.backend"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(projectTests(":compiler:tests-common-new"))

    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))

    junit5Classpath(libs.junit.jupiter.api)
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()

    val localJunit5Classpath: FileCollection = junit5Classpath

    doFirst {
        systemProperty("junit5.classpath", localJunit5Classpath.files.joinToString(",") { it.absolutePath })
    }
}
