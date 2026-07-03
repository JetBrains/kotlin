import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("common-configuration")
    id("test-federation-convention")
    kotlin("jvm")
    id("project-tests-convention")
}

dependencies {
    api(project(":kotlin-scripting-jvm-host-unshaded"))
    implementation(project(":kotlin-compiler-runner-unshaded"))
    implementation(project(":kotlin-daemon-client"))
    implementation(project(":daemon-common"))
    // Only used for the (pure, compiler-independent) SnippetArtifact/SnippetArtifactCodec wire
    // decoding helpers and the scripting plugin's public option id - see DaemonReplSnippetSession's
    // and DaemonReplSnippetCompiler's KDoc.
    implementation(project(":kotlin-scripting-compiler"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(kotlinTest("junit5"))
    // Needed at test runtime because kotlin-scripting-jvm-host-unshaded's own classes reference
    // compiler-core types (e.g. KotlinCompilerVersion); mirrors jsr223-bta's build.gradle.kts.
    testRuntimeOnly(project(":kotlin-compiler"))
}

// The classpath the compile daemon is spawned/discovered with for tests: it must contain a plain
// (unshaded) kotlin-compiler plus the plain (unshaded) scripting-compiler plugin jar, so the
// daemon's own plugin discovery (via the plugin jar's real, un-relocated META-INF/services files)
// picks the scripting K2 compiler plugin registrar up automatically - unlike the BTA-backed engine
// (see libraries/scripting/jsr223-bta), this module never needs to synthesize a `-Xplugin` services
// jar, because it never runs through a shaded/relocated compiler artifact.
val daemonCompilerClasspath by configurations.creating

dependencies {
    add(daemonCompilerClasspath.name, project(":kotlin-compiler"))
    // The plain :kotlin-compiler jar does not bundle the daemon's own main class (that class ships
    // as its own, separate dist jar) - it must be added explicitly for the daemon to be launchable.
    add(daemonCompilerClasspath.name, project(":kotlin-daemon"))
    add(daemonCompilerClasspath.name, project(":kotlin-scripting-compiler"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xallow-kotlin-package")
    }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        dependsOn(":dist")
        workingDir = rootDir
        val classpathProvider = project.provider { daemonCompilerClasspath.files.joinToString(File.pathSeparator) }
        doFirst {
            systemProperty("kotlinJsr223DaemonCompilerClasspath", classpathProvider.get())
        }
    }
}
