plugins {
    kotlin("jvm")
}

val embeddableTestRuntime by configurations.creating

dependencies {
    testApi(commonDep("junit"))

    testApi(project(":kotlin-scripting-js"))
    testApi(project(":compiler:plugin-api"))
    testApi(project(":kotlin-scripting-compiler"))
    testApi(project(":compiler:cli"))
    testApi(project(":compiler:backend.js"))
    testApi(project(":compiler:ir.tree.impl"))
    testApi(project(":js:js.engines"))
    testApi(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntimeOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntimeOnly(intellijDep()) {
        includeJars("idea", "idea_rt", "log4j", "guava", "jdom", rootProject = rootProject)
    }
    testRuntimeOnly(commonDep("org.jetbrains.intellij.deps", "trove4j"))
    testRuntimeOnly(intellijDep()) { includeJars("intellij-deps-fastutil-8.4.1-4") }
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    dependsOn(":kotlin-stdlib-js-ir:compileKotlinJs")
    systemProperty("kotlin.js.full.stdlib.path", "libraries/stdlib/js-ir/build/classes/kotlin/js/main")
    workingDir = rootDir
}
