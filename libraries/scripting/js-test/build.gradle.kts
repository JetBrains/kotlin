
plugins {
    kotlin("jvm")
}

jvmTarget = "1.6"

val embeddableTestRuntime by configurations.creating

dependencies {
    testCompile(commonDep("junit"))

    testCompile(project(":kotlin-scripting-js"))
    testCompile(project(":compiler:plugin-api"))
    testCompile(project(":kotlin-scripting-compiler-unshaded"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:backend.js"))
    testCompile(project(":js:js.engines"))
    testCompile(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntimeOnly(intellijCoreDep()) { includeJars("intellij-core") }
    Platform[193].orLower {
        testRuntimeOnly(intellijDep()) { includeJars("openapi", "picocontainer", rootProject = rootProject) }
    }
    testRuntimeOnly(intellijDep()) {
        includeJars("idea", "idea_rt", "log4j", "guava", "jdom", rootProject = rootProject)
    }
    testRuntimeOnly(commonDep("org.jetbrains.intellij.deps", "trove4j"))
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
