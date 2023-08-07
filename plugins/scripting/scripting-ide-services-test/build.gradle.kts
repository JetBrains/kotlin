
plugins {
    kotlin("jvm")
}

project.updateJvmTarget("1.8")

val allTestsRuntime by configurations.creating

val testApi by configurations
testApi.extendsFrom(allTestsRuntime)

val embeddableTestRuntime by configurations.creating {
    extendsFrom(allTestsRuntime)
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }
}

dependencies {
    allTestsRuntime(libs.junit4)
    testApi(kotlinStdlib("jdk8"))
    testApi(project(":kotlin-scripting-ide-services-unshaded"))
    testApi(project(":kotlin-scripting-compiler"))
    testApi(project(":kotlin-scripting-dependencies-maven"))
    testApi(project(":compiler:cli"))

    testImplementation(commonDependency("org.jetbrains.kotlinx", "kotlinx-coroutines-core"))
    testImplementation(commonDependency("org.jetbrains.kotlinx", "kotlinx-coroutines-core-jvm"))
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testImplementation(project(":analysis:decompiled:decompiler-to-psi"))
    testImplementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    testImplementation(intellijCore())
    testImplementation(projectTests(":analysis:decompiled:decompiler-to-file-stubs"))
    testRuntimeOnly(project(":kotlin-compiler"))
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps", "trove4j"))
    testRuntimeOnly(project(":kotlin-scripting-ide-common")) { isTransitive = false }

    embeddableTestRuntime(project(":kotlin-scripting-ide-services"))
    embeddableTestRuntime(project(":kotlin-scripting-compiler-impl-embeddable"))
    embeddableTestRuntime(project(":kotlin-scripting-dependencies"))
    embeddableTestRuntime(project(":kotlin-scripting-dependencies-maven-all"))
    embeddableTestRuntime(kotlinStdlib("jdk8"))
    embeddableTestRuntime(testSourceSet.output)
    embeddableTestRuntime(commonDependency("org.jetbrains.kotlinx", "kotlinx-coroutines-core"))
    embeddableTestRuntime(commonDependency("org.jetbrains.kotlinx", "kotlinx-coroutines-core-jvm"))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions.freeCompilerArgs += "-Xallow-kotlin-package"
}

projectTest(parallel = true) {
    dependsOn(":kotlin-compiler:distKotlinc")
    workingDir = rootDir
    doFirst {
        systemProperty("kotlin.script.base.compiler.arguments", "-language-version 1.9")
    }
}

// This doesn;t work now due to conflicts between embeddable compiler contents and intellij sdk modules
// To make it work, the dependencies to the intellij sdk should be eliminated
projectTest(taskName = "embeddableTest", parallel = true) {
    workingDir = rootDir
    dependsOn(embeddableTestRuntime)
    classpath = embeddableTestRuntime

    exclude("**/JvmReplIdeTest.class")
    doFirst {
        systemProperty("kotlin.script.base.compiler.arguments", "-language-version 1.9")
    }
}
