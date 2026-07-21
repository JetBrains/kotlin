plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("project-tests-convention")
}

repositories {
    if (!kotlinBuildProperties.isTeamcityBuild.get()) {
        androidXMavenLocal(androidXMavenLocalPath)
    }
}

fun DependencyHandler.testImplementationArtifactOnly(dependency: String) {
    testImplementation(dependency) {
        isTransitive = false
    }
}

optInToObsoleteDescriptorBasedAPI()

kotlin {
    jvmToolchain(11)
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
}

optInToK1Deprecation()

dependencies {
    // junit
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.intellij.fastutil)
    testRuntimeOnly(jpsModelImpl())

    // kotlin deps
    testImplementation(project(":kotlin-stdlib"))
    testImplementation(project(":kotlin-stdlib-common"))
    testImplementation(project(":kotlin-reflect"))
    testImplementation(project(":kotlin-metadata-jvm"))
    testImplementation(kotlinTest("junit5"))
    testImplementation(project(":compiler:ir.backend.common"))
    testImplementation(project(":compiler:cli"))
    testImplementation(project(":compiler:cli-base"))
    testImplementation(project(":compiler:cli-jvm"))
    testImplementation(project(":compiler:backend.jvm"))
    testImplementation(project(":compiler:fir:fir2ir:jvm-backend"))
    testImplementation(project(":compiler:backend.jvm.entrypoint"))
    testImplementation(project(":compiler:config.jvm"))
    testImplementation(project(":compiler:frontend"))
    testImplementation(project(":core:descriptors"))
    testImplementation(project(":core:descriptors.jvm"))
    testImplementation(project(":core:deserialization.common.jvm"))
    testImplementation(project(":core:language.targets.jvm"))
    testImplementation(intellijCore())
    testImplementation(libs.guava)

    // Compose compiler deps
    testImplementation(project(":plugins:compose-compiler-plugin:compiler-hosted"))
    testImplementation(project(":plugins:compose-compiler-plugin:group-mapping"))
    testImplementation(project(":plugins:compose-compiler-plugin:compiler-hosted:runtime-test-utils"))

    // protobuf dependencies for tests
    testImplementation(libs.protobuf.java.lite)
    testImplementation(project(":plugins:compose-compiler-plugin:compiler-hosted:integration-tests:protobuf-test-classes"))

    // coroutines for runtime tests
    testImplementation(commonDependency("org.jetbrains.kotlinx", "kotlinx-coroutines-test-jvm"))

    // runtime tests
    testImplementationArtifactOnly(composeRuntime())
    testImplementationArtifactOnly(composeRuntimeAnnotations())
    testImplementation(libs.androidx.collections)
    testRuntimeOnly(project(":kotlin-script-runtime"))

    // other compose
    testImplementationArtifactOnly(compose("foundation", "foundation"))
    testImplementationArtifactOnly(compose("foundation", "foundation-layout"))
    testImplementationArtifactOnly(compose("animation", "animation"))
    testImplementationArtifactOnly(compose("ui", "ui"))
    testImplementationArtifactOnly(compose("ui", "ui-graphics"))
    testImplementationArtifactOnly(compose("ui", "ui-text"))
    testImplementationArtifactOnly(compose("ui", "ui-unit"))

    // external
    testImplementationArtifactOnly(commonDependency("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm"))
    testImplementationArtifactOnly("com.google.dagger:dagger:2.40.1")
    testImplementation(libs.intellij.asm)
}

projectTests {
    testTask(maxHeapSizeMb = 1024) {
        workingDir = rootDir
        jvmArgs("--add-opens=jdk.jdi/com.sun.tools.jdi=ALL-UNNAMED")
        environment("CI", kotlinBuildProperties.isTeamcityBuild.get())
        if (project.providers.gradleProperty("generate.golden").orElse("false").get().toBooleanStrict()) {
            environment("GENERATE_GOLDEN", "true")
        }
        // runtime tests are executed in this module with compiler built from source (see androidx.compose.compiler.plugins.kotlin.RuntimeTests)
        inputs.dir(File(rootDir, "plugins/compose/compiler-hosted/runtime-tests/src")).withPathSensitivity(PathSensitivity.RELATIVE)
    }

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withTestJar()
    withMockJdkAnnotationsJar()
}

// This task exists only for compatibility with TeamCity config
// For the period when the build was migrated from KMP setup but not merged to master yet
tasks.register("jvmTest")
