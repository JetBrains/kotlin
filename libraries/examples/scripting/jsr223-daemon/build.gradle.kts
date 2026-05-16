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
    // Only used for the scripting plugin's public option id - see DaemonReplCompiler's KDoc.
    implementation(project(":kotlin-scripting-compiler"))
    // ClassId/FqName/NameUtils, used to predict a snippet's wrapper class name and track prior
    // snippets' ClassIds (see DaemonReplCompiler.snippetClassId) - no compiler-internals dependency
    // beyond this pure name/ID utility module.
    implementation(project(":core:compiler.common"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(kotlinTest("junit5"))
    // Needed at test runtime because kotlin-scripting-jvm-host-unshaded's own classes reference
    // compiler-core types (e.g. KotlinCompilerVersion).
    testRuntimeOnly(project(":kotlin-compiler"))
    // The main-kts script definition, used by tests exercising KotlinJsr223DaemonScriptEngineFactory's
    // custom-script-definition support (see KotlinJsr223DaemonScriptEngineImpl's KDoc).
    testImplementation(project(":kotlin-main-kts"))
    // MainKtsScriptDefinition's MainKtsConfigurator instantiates a MavenDependenciesResolver as part
    // of its default constructor argument, so this must be on the classpath even though these tests
    // never exercise dependency resolution themselves.
    testRuntimeOnly(project(":kotlin-scripting-dependencies-maven"))
}

// The classpath the compile daemon is spawned/discovered with for tests: it must contain a plain
// (unshaded) kotlin-compiler plus the plain (unshaded) scripting-compiler plugin jar, so the
// daemon's own plugin discovery (via the plugin jar's real, un-relocated META-INF/services files)
// picks the scripting K2 compiler plugin registrar up automatically - this module never needs to
// synthesize a `-Xplugin` services jar, because it never runs through a shaded/relocated compiler
// artifact.
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
    testTask() {
        dependsOn(":dist")
        workingDir = rootDir
        val classpathProvider = project.provider { daemonCompilerClasspath.files.joinToString(File.pathSeparator) }
        doFirst {
            systemProperty("kotlinJsr223DaemonCompilerClasspath", classpathProvider.get())
        }
    }
}
