plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation(kotlinStdlib("jdk8"))
    testImplementation(project(":kotlin-test:kotlin-test-junit5"))
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(projectTests(":compiler:tests-common-new"))
}

val defaultSnapshotVersion: String by extra
findProperty("deployVersion")?.let {
    assert(findProperty("build.number") != null) { "`build.number` parameter is expected to be explicitly set with the `deployVersion`" }
}
val buildNumber by extra(findProperty("build.number")?.toString() ?: defaultSnapshotVersion)
val kotlinVersion by extra(
    findProperty("deployVersion")?.toString()?.let { deploySnapshotStr ->
        if (deploySnapshotStr != "default.snapshot") deploySnapshotStr else defaultSnapshotVersion
    } ?: buildNumber
)

projectTest(jUnitMode = JUnitMode.JUnit5) {
    workingDir = rootDir
    useJUnitPlatform { }
    doFirst {
        val defaultMavenLocal = rootProject.projectDir.resolve("build/repo").absolutePath
        val mavenLocal = System.getProperty("maven.repo.local") ?: defaultMavenLocal
        systemProperty("maven.repo.local", mavenLocal)
        systemProperty("kotlin.version", kotlinVersion)
    }
}
