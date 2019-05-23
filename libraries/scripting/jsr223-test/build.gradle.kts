
plugins {
    kotlin("jvm")
}

jvmTarget = "1.6"

val embeddableTestRuntime by configurations.creating

dependencies {
    testCompile(commonDep("junit"))
    testCompileOnly(project(":kotlin-scripting-jvm-host"))
    testCompileOnly(project(":compiler:cli"))
    testCompileOnly(project(":core:util.runtime"))
    testRuntime(project(":kotlin-scripting-jsr223"))
    embeddableTestRuntime(commonDep("junit"))
    embeddableTestRuntime(project(":kotlin-scripting-jsr223-embeddable"))
    embeddableTestRuntime(testSourceSet.output)
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

projectTest(parallel = true)

projectTest(taskName = "embeddableTest", parallel = true) {
    workingDir = rootDir
    dependsOn(embeddableTestRuntime)
    classpath = embeddableTestRuntime
}
