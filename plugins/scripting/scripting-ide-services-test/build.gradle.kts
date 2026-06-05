import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("project-tests-convention")
}

project.updateJvmTarget("1.8")

val allTestsRuntime = configurations.create("allTestsRuntime")

val testApi = configurations.getByName("testApi")
testApi.extendsFrom(allTestsRuntime)

val embeddableTestRuntime = configurations.create("embeddableTestRuntime") {
    extendsFrom(allTestsRuntime)
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }
}

dependencies {
    testImplementation(kotlinStdlib("jdk8"))
    testImplementation(project(":kotlin-scripting-ide-services-unshaded"))
    testImplementation(project(":kotlin-scripting-compiler"))
    testImplementation(project(":kotlin-scripting-dependencies-maven"))
    testImplementation(project(":compiler:cli"))

    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.core.jvm)
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testImplementation(project(":analysis:decompiled:decompiler-to-psi"))
    testImplementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    testImplementation(intellijCore())
    testImplementation(testFixtures(project(":analysis:decompiled:decompiler-to-file-stubs")))
    testRuntimeOnly(project(":kotlin-compiler"))
    testRuntimeOnly(project(":kotlin-scripting-ide-common")) { isTransitive = false }

    embeddableTestRuntime(project(":kotlin-scripting-ide-services"))
    embeddableTestRuntime(project(":kotlin-scripting-compiler-impl-embeddable"))
    embeddableTestRuntime(project(":kotlin-scripting-dependencies"))
    embeddableTestRuntime(project(":kotlin-scripting-dependencies-maven-all"))
    embeddableTestRuntime(kotlinStdlib("jdk8"))
    embeddableTestRuntime(testSourceSet.output)
    embeddableTestRuntime(libs.kotlinx.coroutines.core)
    embeddableTestRuntime(libs.kotlinx.coroutines.core.jvm)
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xallow-kotlin-package")
}

projectTests {
    testTask {
        dependsOn(":kotlin-compiler:distKotlinc")
        workingDir = rootDir
        doFirst {
            systemProperty("kotlin.script.base.compiler.arguments", "-language-version 1.9")
        }
    }

    // This doesn;t work now due to conflicts between embeddable compiler contents and intellij sdk modules
    // To make it work, the dependencies to the intellij sdk should be eliminated
    testTask("embeddableTest", skipInLocalBuild = false) {
        workingDir = rootDir
        dependsOn(embeddableTestRuntime)
        classpath = embeddableTestRuntime

        exclude("**/JvmReplIdeTest.class")
        doFirst {
            systemProperty("kotlin.script.base.compiler.arguments", "-language-version 1.9")
        }
    }
}
