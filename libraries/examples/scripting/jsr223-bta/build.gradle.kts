import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("common-configuration")
    id("test-federation-convention")
    kotlin("jvm")
    id("project-tests-convention")
}

dependencies {
    api(project(":kotlin-scripting-jvm-host-unshaded"))
    implementation(project(":compiler:build-tools:kotlin-build-tools-api"))
    // Only used for the scripting plugin's public option id.
    implementation(project(":kotlin-scripting-compiler"))
    // ClassId/FqName/NameUtils only.
    implementation(project(":core:compiler.common"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(kotlinTest("junit5"))
    // kotlin-scripting-jvm-host-unshaded's classes reference compiler-core types.
    testRuntimeOnly(project(":kotlin-compiler"))
    testImplementation(project(":kotlin-main-kts"))
    // MainKtsConfigurator instantiates a MavenDependenciesResolver in a default constructor
    // argument, so this is needed even though no test resolves dependencies.
    testRuntimeOnly(project(":kotlin-scripting-dependencies-maven"))
}

// The Build Tools API implementation plus the compiler it runs on, loaded into an isolated
// classloader. The scripting compiler plugin cannot be part of it: the implementation jar embeds
// only a relocated scripting compiler, with its plugin service files stripped.
val btaImplClasspath = configurations.create("btaImplClasspath")

// The scripting plugin jar, which has to be the regular (non-relocated) embeddable artifact to match
// the embeddable compiler above.
val scriptingCompilerPluginClasspath = configurations.create("scriptingCompilerPluginClasspath")

dependencies {
    add(btaImplClasspath.name, project(":compiler:build-tools:kotlin-build-tools-impl"))
    add(btaImplClasspath.name, project(":compiler:build-tools:kotlin-build-tools-compat"))
    add(scriptingCompilerPluginClasspath.name, project(":kotlin-scripting-compiler-embeddable"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
    }
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
        val btaImplClasspathProvider = project.provider { btaImplClasspath.files.joinToString(File.pathSeparator) }
        val scriptingPluginClasspathProvider =
            project.provider { scriptingCompilerPluginClasspath.files.joinToString(File.pathSeparator) }
        doFirst {
            systemProperty("kotlinJsr223BtaImplClasspath", btaImplClasspathProvider.get())
            systemProperty("kotlinJsr223BtaScriptingPluginClasspath", scriptingPluginClasspathProvider.get())
        }
    }
}
