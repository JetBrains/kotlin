
description = "Annotation Processor for Kotlin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:cli"))
    api(project(":compiler:backend"))
    api(project(":compiler:frontend"))
    api(project(":compiler:frontend.java"))
    api(project(":compiler:plugin-api"))

    compileOnly(toolsJarApi())
    compileOnly(project(":kotlin-annotation-processing-cli"))
    compileOnly(project(":kotlin-annotation-processing-base"))
    compileOnly(project(":kotlin-annotation-processing-runtime"))
    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))

    testImplementation(intellijCore())
    testRuntimeOnly(intellijResources()) { isTransitive = false }

    testApi(projectTests(":compiler:tests-common"))
    testApi(project(":kotlin-annotation-processing-base"))
    testApi(projectTests(":kotlin-annotation-processing-base"))
    testApi(commonDependency("junit:junit"))
    testApi(project(":kotlin-annotation-processing-runtime"))

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
