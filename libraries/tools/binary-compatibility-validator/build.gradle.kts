plugins {
    kotlin("jvm")
    id("test-inputs-check")
}

val testArtifacts by configurations.creating {
    isTransitive = false
}
val nativeStdlib by configurations.creating {
    isTransitive = false
}

dependencies {
    api(libs.kotlinx.bcv)
    runtimeOnly("org.ow2.asm:asm-tree:9.7")
    runtimeOnly("org.jetbrains.kotlin:kotlin-metadata-jvm:${project.bootstrapKotlinVersion}")
    if (kotlinBuildProperties.isKotlinNativeEnabled.get()) {
        runtimeOnly(project(":kotlin-compiler-embeddable"))
    } else {
        runtimeOnly(kotlin("compiler-embeddable", bootstrapKotlinVersion))
    }

    testImplementation(kotlinTest("junit"))
    testImplementation(testFixtures(project(":compiler:test-infrastructure-utils")))

    testArtifacts(project(":kotlin-stdlib"))
    testArtifacts(project(":kotlin-stdlib-jdk7"))
    testArtifacts(project(":kotlin-stdlib-jdk8"))
    testArtifacts(project(":kotlin-reflect"))
    testArtifacts(project(":kotlin-stdlib", "jsRuntimeElements"))
    testArtifacts(project(":kotlin-stdlib", "wasmJsRuntimeElements"))
    testArtifacts(project(":kotlin-stdlib", "wasmWasiRuntimeElements"))
    if (kotlinBuildProperties.isKotlinNativeEnabled.get()) {
        nativeStdlib(project(":kotlin-native:runtime", "nativeStdlib"))
    }
}

sourceSets {
    "test" {
        java {
            srcDir("src/test/kotlin")
        }
    }
}

tasks.test {
    systemProperty("native.enabled", kotlinBuildProperties.isKotlinNativeEnabled.get())
    systemProperty("overwrite.output", project.providers.gradleProperty("overwrite.output").orNull ?: System.getProperty("overwrite.output", "false"))
    systemProperty("kotlinVersion", project.version)
    jvmArgs("-ea")
    addClasspathProperty("testArtifacts") {
        from(testArtifacts)
    }
    addDirectoryProperty("reference-public-api") {
        set(project.layout.projectDirectory.dir("reference-public-api"))
    }
    addDirectoryProperty("klib-public-api") {
        set(project.layout.projectDirectory.dir("klib-public-api"))
    }
    addClasspathProperty("testCasesClassesDirs") {
        from(sourceSets["test"].output.classesDirs)
    }
    inputs.dir(layout.projectDirectory.dir("src/test/kotlin/cases"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("testCasesDir")
    addClasspathProperty(nativeStdlib, "nativeStdlib")
}
