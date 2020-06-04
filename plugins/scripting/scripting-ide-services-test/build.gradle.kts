
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
    testCompile(project(":kotlin-scripting-ide-services-unshaded"))
    testCompile(project(":kotlin-scripting-compiler"))
    testCompile(project(":kotlin-scripting-dependencies"))
    testCompile(project(":kotlin-main-kts"))
    testCompile(project(":compiler:cli-common"))

    testRuntimeOnly(project(":kotlin-compiler"))
    testRuntimeOnly(commonDep("org.jetbrains.intellij.deps", "trove4j"))
    testRuntimeOnly(project(":idea:ide-common")) { isTransitive = false }

    embeddableTestRuntime(project(":kotlin-scripting-ide-services"))
    embeddableTestRuntime(project(":kotlin-compiler-embeddable"))
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
    dependsOn(":dist")
    workingDir = rootDir
}

// This doesn;t work now due to conflicts between embeddable compiler contents and intellij sdk modules
// To make it work, the dependencies to the intellij sdk should be eliminated
projectTest(taskName = "embeddableTest", parallel = true) {
    workingDir = rootDir
    dependsOn(embeddableTestRuntime)
    classpath = embeddableTestRuntime
}
