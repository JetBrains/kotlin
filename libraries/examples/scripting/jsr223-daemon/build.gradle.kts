import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("common-configuration")
    id("test-federation-convention")
    kotlin("jvm")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    api(project(":kotlin-scripting-jvm-host-unshaded"))
    implementation(project(":kotlin-compiler-runner-unshaded"))
    implementation(project(":kotlin-daemon-client"))
    implementation(project(":daemon-common"))
    // Only used for the scripting plugin's public option id.
    implementation(project(":kotlin-scripting-compiler"))
    // ClassId/FqName/NameUtils only, see DaemonReplCompiler.snippetClassId.
    implementation(project(":core:compiler.common"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(kotlinTest("junit5"))
    // kotlin-scripting-jvm-host-unshaded's classes reference compiler-core types at runtime.
    testRuntimeOnly(project(":kotlin-compiler"))
    testImplementation(project(":kotlin-main-kts"))
    // MainKtsConfigurator instantiates a MavenDependenciesResolver in a default constructor argument.
    testRuntimeOnly(project(":kotlin-scripting-dependencies-maven"))
}

// Must contain unshaded jars, so the daemon's plugin discovery picks the scripting registrar up
// from their un-relocated META-INF/services files.
val daemonCompilerClasspath = configurations.create("daemonCompilerClasspath")

dependencies {
    add(daemonCompilerClasspath.name, project(":kotlin-compiler"))
    // :kotlin-compiler does not bundle the daemon's main class, needed to launch the daemon.
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
