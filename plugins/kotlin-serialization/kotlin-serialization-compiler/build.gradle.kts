description = "Kotlin Serialization Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "asm-all", rootProject = rootProject) }

    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:ir.psi2ir"))
    compileOnly(project(":js:js.frontend"))
    compileOnly(project(":js:js.translator"))

    runtime(kotlinStdlib())

    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0-1.4.0-dev-5730") { isTransitive = false }

    testRuntimeOnly(intellijCoreDep()) { includeJars("intellij-core") }

    Platform[192].orHigher {
        testRuntimeOnly(intellijDep()) { includeJars("platform-concurrency") }
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

projectTest(parallel = true) {
    workingDir = rootDir
}

apply(from = "$rootDir/gradle/kotlinPluginPublication.gradle.kts")
