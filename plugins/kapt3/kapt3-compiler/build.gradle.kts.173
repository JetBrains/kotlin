
description = "Annotation Processor for Kotlin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntime(intellijDep())
    testCompileOnly(intellijDep()) { includeJars("idea", "idea_rt", "openapi") }

    compile(project(":compiler:util"))
    compile(project(":compiler:cli"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:plugin-api"))
    compileOnly(project(":kotlin-annotation-processing-base"))
    compileOnly(project(":kotlin-annotation-processing-runtime"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("asm-all") }

    testCompile(projectTests(":compiler:tests-common"))
    testCompile(project(":kotlin-annotation-processing-base"))
    testCompile(projectTests(":kotlin-annotation-processing-base"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-annotation-processing-runtime"))

    embeddedComponents(project(":kotlin-annotation-processing-runtime")) { isTransitive = false }
    embeddedComponents(project(":kotlin-annotation-processing-base")) { isTransitive = false }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar {}

projectTest {
    workingDir = rootDir
    dependsOn(":dist")
}

runtimeJar {
    fromEmbeddedComponents()
}

sourcesJar()
javadocJar()

dist()

publish()
