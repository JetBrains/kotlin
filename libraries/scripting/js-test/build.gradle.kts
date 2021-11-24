plugins {
    kotlin("jvm")
}

val embeddableTestRuntime by configurations.creating

dependencies {
    testApi(commonDependency("junit"))

    testApi(project(":kotlin-scripting-js"))
    testApi(project(":compiler:plugin-api"))
    testApi(project(":kotlin-scripting-compiler"))
    testApi(project(":compiler:cli"))
    testApi(project(":compiler:backend.js"))
    testApi(project(":compiler:ir.tree.impl"))
    testApi(project(":js:js.engines"))
    testApi(intellijCore())

    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps:jdom"))
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps:trove4j"))
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
