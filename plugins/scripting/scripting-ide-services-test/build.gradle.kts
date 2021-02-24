
plugins {
    kotlin("jvm")
}

jvmTarget = "1.8"

val allTestsRuntime by configurations.creating
val testCompile by configurations
testCompile.extendsFrom(allTestsRuntime)
val embeddableTestRuntime by configurations.creating {
    extendsFrom(allTestsRuntime)
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }
}

dependencies {
    allTestsRuntime(commonDep("junit"))
    testCompile(kotlinStdlib("jdk8"))
    testCompile(project(":kotlin-scripting-ide-services-unshaded"))
    testCompile(project(":kotlin-scripting-compiler"))
    testCompile(project(":kotlin-scripting-dependencies"))
    testCompile(project(":kotlin-main-kts"))
    testCompile(project(":compiler:cli"))

    testRuntimeOnly(project(":kotlin-compiler"))
    testRuntimeOnly(commonDep("org.jetbrains.intellij.deps", "trove4j"))
    testRuntimeOnly(project(":idea:ide-common")) { isTransitive = false }

    embeddableTestRuntime(project(":kotlin-scripting-ide-services", configuration="runtimeElements"))
    embeddableTestRuntime(project(":kotlin-scripting-compiler-impl-embeddable", configuration="runtimeElements"))
    embeddableTestRuntime(project(":kotlin-scripting-dependencies", configuration="runtimeElements"))
    // For tests with IvyResolver
    embeddableTestRuntime(project(":kotlin-main-kts"))
    embeddableTestRuntime(testSourceSet.output)
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
}

// This doesn;t work now due to conflicts between embeddable compiler contents and intellij sdk modules
// To make it work, the dependencies to the intellij sdk should be eliminated
projectTest(taskName = "embeddableTest", parallel = true) {
    workingDir = rootDir
    dependsOn(embeddableTestRuntime)
    classpath = embeddableTestRuntime
}
