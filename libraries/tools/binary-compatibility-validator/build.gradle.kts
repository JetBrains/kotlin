plugins {
    kotlin("jvm")
}

val testArtifacts by configurations.creating

dependencies {
    api(libs.kotlinx.bcv)
    runtimeOnly("org.ow2.asm:asm-tree:9.7")
    runtimeOnly("org.jetbrains.kotlin:kotlin-metadata-jvm:${project.bootstrapKotlinVersion}")
    if (kotlinBuildProperties.isKotlinNativeEnabled) {
        runtimeOnly(project(":kotlin-compiler-embeddable"))
    } else {
        runtimeOnly(kotlin("compiler-embeddable", bootstrapKotlinVersion))
    }

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

val test by tasks.existing(Test::class) {
    dependsOn(testArtifacts)
    dependsOn(":kotlin-stdlib:assemble")
    if (kotlinBuildProperties.isKotlinNativeEnabled) {
        dependsOn(":kotlin-native:runtime:nativeStdlib")
    }

    systemProperty("native.enabled", kotlinBuildProperties.isKotlinNativeEnabled)
    systemProperty("overwrite.output", project.providers.gradleProperty("overwrite.output").orNull ?: System.getProperty("overwrite.output", "false"))
    systemProperty("kotlinVersion", project.version)
    systemProperty("testCasesClassesDirs", sourceSets["test"].output.classesDirs.asPath)
    jvmArgs("-ea")
}
