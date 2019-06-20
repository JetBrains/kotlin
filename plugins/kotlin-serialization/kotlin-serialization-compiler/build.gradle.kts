description = "Kotlin Serialization Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "asm-all", rootProject = rootProject) }

    compile(project(":compiler:plugin-api"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:ir.backend.common"))
    compile(project(":js:js.frontend"))
    compile(project(":js:js.translator"))

    runtime(kotlinStdlib())

    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
    testCompile("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.11.0")

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
testsJar {}

dist(targetName = the<BasePluginConvention>().archivesBaseName + ".jar")

projectTest(parallel = true) {
    workingDir = rootDir
}
