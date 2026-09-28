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
    implementation(project(":kotlin-scripting-compiler"))
    implementation(project(":core:compiler.common"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(kotlinTest("junit5"))
    testRuntimeOnly(project(":kotlin-compiler"))
    testImplementation(project(":kotlin-main-kts"))
    testRuntimeOnly(project(":kotlin-scripting-dependencies-maven"))
}

val daemonCompilerClasspath = configurations.create("daemonCompilerClasspath")

dependencies {
    add(daemonCompilerClasspath.name, project(":kotlin-compiler"))
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
