
plugins {
    kotlin("jvm")
}

jvmTarget = "1.6"

val embeddableTestRuntime by configurations.creating

dependencies {
    testCompile(commonDep("junit"))

    testCompile(project(":kotlin-scripting-js"))
    testCompile(project(":compiler:plugin-api"))
    testCompile(project(":kotlin-scripting-compiler"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:backend.js"))
    testCompile(project(":js:js.engines"))
    testCompile(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntimeOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntimeOnly(intellijDep()) {
        includeJars("openapi", "idea", "idea_rt", "log4j", "picocontainer", "guava", "jdom", rootProject = rootProject)
    }
    testRuntimeOnly(commonDep("org.jetbrains.intellij.deps", "trove4j"))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    workingDir = rootDir
}
