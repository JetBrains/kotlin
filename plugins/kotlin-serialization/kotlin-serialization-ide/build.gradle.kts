
description = "Kotlinx Serialization IDEA Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":js:js.translator"))

    compile(project(":kotlinx-serialization-compiler-plugin"))
    compile(project(":idea"))
    compile(project(":idea:idea-gradle"))
    compile(project(":idea:idea-maven"))
    compile(project(":plugins:annotation-based-compiler-plugins-ide-support"))
    compileOnly(intellijDep())
    compileOnly(intellijPluginDep("java"))
    excludeInAndroidStudio(rootProject) { compileOnly(intellijPluginDep("maven")) }
    compileOnly(intellijPluginDep("gradle"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-jvm:1.0-M1-1.4.0-rc") { isTransitive = false }

    testRuntimeOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntimeOnly(intellijDep()) { includeJars("platform-concurrency") }

    testCompileOnly(intellijDep())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()
testsJar()

projectTest(parallel = true) {
    workingDir = rootDir
}
