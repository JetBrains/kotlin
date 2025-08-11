plugins {
    kotlin("jvm")
}

val testArtifacts by configurations.creating

dependencies {
    implementation(project(":libraries:tools:abi-validation:abi-tools-api"))
    runtimeOnly(project(":libraries:tools:abi-validation:abi-tools"))
    /*
    implementation("org.jetbrains.kotlin:abi-tools-api:${project.bootstrapKotlinVersion}")
    runtimeOnly("org.jetbrains.kotlin:abi-tools:${project.bootstrapKotlinVersion}")
     */

    testApi(kotlinTest("junit"))

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

tasks.test {
    dependsOn(testArtifacts)
    dependsOn(":kotlin-stdlib:assemble")
    if (kotlinBuildProperties.isKotlinNativeEnabled) {
        dependsOn(":kotlin-native:runtime:nativeStdlib")
    }

    systemProperty("native.enabled", kotlinBuildProperties.isKotlinNativeEnabled)
    systemProperty("overwrite.output", project.providers.gradleProperty("overwrite.output").orNull ?: System.getProperty("overwrite.output", "false"))
    systemProperty("kotlinVersion", project.version)
    systemProperty("testCasesClassesDirs", sourceSets.test.get().output.classesDirs.asPath)
    jvmArgs("-ea")
}
