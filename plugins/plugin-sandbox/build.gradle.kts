import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:fir:cones"))
    compileOnly(project(":compiler:fir:tree"))
    compileOnly(project(":compiler:fir:resolve"))
    compileOnly(project(":compiler:fir:plugin-utils"))
    compileOnly(project(":compiler:fir:checkers"))
    compileOnly(project(":compiler:fir:fir2ir"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:fir:analysis-tests"))
    testApi(projectTests(":js:js.tests"))
    testApi(project(":compiler:fir:checkers"))
    testApi(project(":compiler:fir:checkers:checkers.jvm"))
    testApi(project(":compiler:fir:checkers:checkers.js"))
    testApi(project(":compiler:fir:checkers:checkers.native"))
    testApi(project(":compiler:fir:checkers:checkers.wasm"))
    testApi(project(":compiler:fir:plugin-utils"))

    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))

    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.jna:jna"))
    testRuntimeOnly(intellijJDom())
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps:trove4j"))
    testRuntimeOnly(libs.intellij.fastutil)

    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))

    testRuntimeOnly(toolsJar())
}

optInToExperimentalCompilerApi()
optInToUnsafeDuringIrConstructionAPI()

val generationRoot = projectDir.resolve("tests-gen")

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {
        projectDefault()
        this.java.srcDir(generationRoot.name)
    }
}

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(plugin = "idea")
    idea {
        this.module.generatedSourceDirs.add(generationRoot)
    }
}

projectTest(parallel = true, jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
}.also { confugureFirPluginAnnotationsDependency(it) }

testsJar()
