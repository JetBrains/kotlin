plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("project-tests-convention")
}

dependencies {
    testImplementation(kotlinStdlib("jdk8"))
    testImplementation(kotlinTest("junit5"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(testFixtures(project(":compiler:tests-common-new")))
    testImplementation(libs.kotlinx.serialization.json)
}

val defaultSnapshotVersion: String by extra
findProperty("deployVersion")?.let {
    assert(findProperty("build.number") != null) { "`build.number` parameter is expected to be explicitly set with the `deployVersion`" }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        workingDir = rootDir
        val buildNumber by extra(findProperty("build.number")?.toString() ?: defaultSnapshotVersion)
        val kotlinVersion by extra(
            findProperty("deployVersion")?.toString()?.let { deploySnapshotStr ->
                if (deploySnapshotStr != "default.snapshot") deploySnapshotStr else defaultSnapshotVersion
            } ?: buildNumber
        )
        val defaultMavenLocal: String = rootProject.projectDir.resolve("build/repo").absolutePath
        val mavenLocal = System.getProperty("maven.repo.local") ?: defaultMavenLocal
        val defaultKotlincArtifactPath: String = rootProject.projectDir.resolve("dist/kotlinc").absolutePath
        val kotlincArtifactPath = System.getProperty("kotlinc.dist.path") ?: defaultKotlincArtifactPath
        doFirst {
            systemProperty("maven.repo.local", mavenLocal)
            systemProperty("kotlinc.dist.path", kotlincArtifactPath)
            systemProperty("kotlin.version", kotlinVersion)
        }
    }
}
