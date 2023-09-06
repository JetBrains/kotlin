import java.nio.file.Paths

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testImplementation(kotlinStdlib("jdk8"))
    testImplementation(project(":kotlin-test:kotlin-test-junit5"))
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(projectTests(":compiler:tests-common-new"))
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    workingDir = rootDir
    useJUnitPlatform { }
    doFirst {
        val defaultMavenLocal = Paths.get(System.getProperty("user.home"), ".m2", "repository").toAbsolutePath()
        val mavenLocal = System.getProperty("maven.repo.local") ?: defaultMavenLocal
        systemProperty("maven.repo.local", mavenLocal)
        systemProperty("kotlin.version", version)
    }
}
