
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
    testCompile(project(":js:js.engines"))
    testRuntimeOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntimeOnly(intellijDep()) { includeJars("openapi", "idea", "idea_rt", "log4j", "picocontainer-1.2", "guava-25.1-jre", "jdom") }
    testRuntimeOnly(commonDep("org.jetbrains.intellij.deps", "trove4j"))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    workingDir = rootDir
}
