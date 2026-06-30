import org.jetbrains.kotlin.testFederation.SmokeTestConfig
import org.jetbrains.kotlin.testFederation.TemporaryTestFederationApi
import org.jetbrains.kotlin.testFederation.smokeTestConfig

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
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

findProperty("deployVersion")?.let {
    assert(findProperty("build.number") != null) { "`build.number` parameter is expected to be explicitly set with the `deployVersion`" }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        workingDir = rootDir

        @OptIn(TemporaryTestFederationApi::class)
        smokeTestConfig = SmokeTestConfig.RunAllTests

        val kotlinVersion = kotlinBuildProperties.kotlinVersion.get()
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
