
plugins {
    kotlin("jvm")
}

jvmTarget = "1.6"

val embeddableTestRuntime by configurations.creating

dependencies {
    testCompile(commonDep("junit"))
    testCompileOnly(project(":kotlin-scripting-jvm-host-unshaded"))
    testCompileOnly(project(":compiler:cli"))
    testCompileOnly(project(":core:util.runtime"))
    
    testRuntimeOnly(project(":kotlin-scripting-jsr223-unshaded"))
    testRuntimeOnly(project(":kotlin-compiler"))
    testRuntimeOnly(project(":kotlin-reflect"))
    
    embeddableTestRuntime(commonDep("junit"))
    embeddableTestRuntime(project(":kotlin-scripting-jsr223"))
    embeddableTestRuntime(testSourceSet.output)
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions.freeCompilerArgs += "-Xallow-kotlin-package"
}

projectTest(parallel = true)

projectTest(taskName = "embeddableTest", parallel = true) {
    workingDir = rootDir
    dependsOn(embeddableTestRuntime)
    classpath = embeddableTestRuntime
}
