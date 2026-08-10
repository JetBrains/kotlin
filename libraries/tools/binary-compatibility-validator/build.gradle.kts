import org.jetbrains.kotlin.testFederation.SmokeTestConfig.Companion.RunAllTests
import org.jetbrains.kotlin.testFederation.smokeTestConfig

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

val testArtifacts = configurations.create("testArtifacts")

dependencies {
    api(libs.kotlinx.bcv)
    runtimeOnly("org.ow2.asm:asm-tree:9.7")
    runtimeOnly("org.jetbrains.kotlin:kotlin-metadata-jvm:${project.bootstrapKotlinVersion}")
    if (kotlinBuildProperties.isKotlinNativeEnabled.get()) {
        runtimeOnly(project(":kotlin-compiler-embeddable"))
    } else {
        runtimeOnly(kotlin("compiler-embeddable", bootstrapKotlinVersion))
    }

    testImplementation(kotlinTest("junit5"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(kotlinStdlib())
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    testArtifacts(project(":kotlin-stdlib"))
    testArtifacts(project(":kotlin-stdlib-jdk7"))
    testArtifacts(project(":kotlin-stdlib-jdk8"))
    testArtifacts(project(":kotlin-reflect"))
}

sourceSets {
    "test" {
        java {
            srcDir("src/test/kotlin")
        }
    }
}

val test = tasks.named("test", Test::class) {
    useJUnitPlatform()
    dependsOn(testArtifacts)
    dependsOn(":kotlin-stdlib:assemble")
    if (kotlinBuildProperties.isKotlinNativeEnabled.get()) {
        dependsOn(":kotlin-native:runtime:nativeStdlib")
    }

    systemProperty("native.enabled", kotlinBuildProperties.isKotlinNativeEnabled.get())
    systemProperty("overwrite.output", project.providers.gradleProperty("overwrite.output").orNull ?: System.getProperty("overwrite.output", "false"))
    systemProperty("kotlinVersion", project.version)
    systemProperty("testCasesClassesDirs", sourceSets["test"].output.classesDirs.asPath)
    jvmArgs("-ea")

    smokeTestConfig = RunAllTests
}
