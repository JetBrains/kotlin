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
    testImplementation(project(":kotlin-script-runtime")) // marker class to locate the jar for legacy-template tests
    testRuntimeOnly(project(":kotlin-scripting-dependencies-maven"))
}

val daemonCompilerClasspath = configurations.create("daemonCompilerClasspath")
val kotlinxSerializationCompilerPluginClasspath = configurations.create("kotlinxSerializationCompilerPluginClasspath")
val allOpenCompilerPluginClasspath = configurations.create("allOpenCompilerPluginClasspath")

dependencies {
    add(daemonCompilerClasspath.name, project(":kotlin-compiler"))
    add(daemonCompilerClasspath.name, project(":kotlin-daemon"))
    add(daemonCompilerClasspath.name, project(":kotlin-scripting-compiler"))
    add(kotlinxSerializationCompilerPluginClasspath.name, project(":kotlinx-serialization-compiler-plugin.embeddable")) {
        isTransitive = false
    }
    add(allOpenCompilerPluginClasspath.name, project(":kotlin-allopen-compiler-plugin")) { isTransitive = false }
    testImplementation(project(":kotlin-scripting-dependencies-maven"))
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
        val serializationPluginClasspathFiles: FileCollection = kotlinxSerializationCompilerPluginClasspath
        inputs.files(serializationPluginClasspathFiles)
            .withPropertyName("kotlinxSerializationCompilerPluginClasspath")
            .withNormalizer(ClasspathNormalizer::class)
        val allOpenPluginClasspathFiles: FileCollection = allOpenCompilerPluginClasspath
        inputs.files(allOpenPluginClasspathFiles)
            .withPropertyName("allOpenCompilerPluginClasspath")
            .withNormalizer(ClasspathNormalizer::class)
        doFirst {
            systemProperty("kotlinJsr223DaemonCompilerClasspath", classpathProvider.get())
            systemProperty("kotlin.script.test.kotlinx.serialization.plugin.classpath", serializationPluginClasspathFiles.asPath)
            systemProperty("kotlin.script.test.allopen.plugin.classpath", allOpenPluginClasspathFiles.asPath)
        }
    }
}
