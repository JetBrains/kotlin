
description = "Annotation Processor for Kotlin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntime(intellijDep())
    testCompileOnly(intellijDep()) { includeJars("idea", "idea_rt") }
    Platform[193].orLower {
        testCompileOnly(intellijDep()) { includeJars("openapi", rootProject = rootProject) }
    }

    testCompileOnly(intellijDep()) { includeJars("platform-api", "platform-impl") }

    Platform[192].orHigher {
        testRuntime(intellijPluginDep("java"))
    }

    compile(project(":compiler:util"))
    compile(project(":compiler:cli"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:plugin-api"))

    compileOnly(toolsJarApi())
    compileOnly(project(":kotlin-annotation-processing-cli"))
    compileOnly(project(":kotlin-annotation-processing-base"))
    compileOnly(project(":kotlin-annotation-processing-runtime"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("asm-all", rootProject = rootProject) }

    testCompile(projectTests(":compiler:tests-common"))
    testCompile(project(":kotlin-annotation-processing-base"))
    testCompile(projectTests(":kotlin-annotation-processing-base"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-annotation-processing-runtime"))

    testCompileOnly(toolsJarApi())
    testRuntimeOnly(toolsJar())

    embedded(project(":kotlin-annotation-processing-runtime")) { isTransitive = false }
    embedded(project(":kotlin-annotation-processing-cli")) { isTransitive = false }
    embedded(project(":kotlin-annotation-processing-base")) { isTransitive = false }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar {}

projectTest(parallel = true) {
    workingDir = rootDir
    dependsOn(":dist")
}

publish()

runtimeJar()

sourcesJar()
javadocJar()
