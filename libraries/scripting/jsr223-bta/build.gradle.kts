import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("common-configuration")
    id("test-federation-convention")
    kotlin("jvm")
    id("project-tests-convention")
}

dependencies {
    api(project(":kotlin-scripting-jvm-host-unshaded"))
    api(project(":compiler:build-tools:kotlin-build-tools-api"))
    // Only used for the (pure, compiler-independent) SnippetArtifact/SnippetArtifactCodec wire
    // decoding helpers - see BtaReplSnippetSession's KDoc.
    implementation(project(":kotlin-scripting-compiler"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(kotlinTest("junit5"))
    // Needed at test runtime because kotlin-scripting-jvm-host-unshaded's own classes reference
    // compiler-core types (e.g. KotlinCompilerVersion); mirrors jsr223-test's build.gradle.kts.
    testRuntimeOnly(project(":kotlin-compiler"))
}

// A Build Tools API implementation classpath for tests to hand to
// `KotlinToolchains.loadImplementation(List<Path>)`. Kept as its own configuration (rather than
// reusing testRuntimeClasspath) so the engine under test loads the implementation the same way a
// real embedder would - from an explicit, self-contained classpath. Mirrors exactly
// `kotlin-build-tools-api-tests`'s `buildToolsApiImpl` configuration (compat + impl + cri-impl) --
// each already transitively pulls in its own compiler/scripting-plugin dependencies; adding
// `:kotlin-compiler` (or other compiler jars) directly on top of this would put a second,
// conflicting copy of the compiler on the daemon's classpath and break it.
val btaImplClasspath by configurations.creating

dependencies {
    add(btaImplClasspath.name, project(":compiler:build-tools:kotlin-build-tools-compat"))
    add(btaImplClasspath.name, project(":compiler:build-tools:kotlin-build-tools-impl"))
    add(btaImplClasspath.name, project(":compiler:build-tools:kotlin-build-tools-cri-impl"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xallow-kotlin-package")
        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
    }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        dependsOn(":dist")
        workingDir = rootDir
        val classpathProvider = project.provider { btaImplClasspath.files.joinToString(File.pathSeparator) }
        doFirst {
            systemProperty("kotlinBtaImplClasspath", classpathProvider.get())
        }
    }
}
