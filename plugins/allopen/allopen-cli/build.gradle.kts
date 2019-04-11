
description = "Kotlin AllOpen Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    runtime(kotlinStdlib())

    testRuntimeOnly(intellijDep()) {
        includeJars("guava", rootProject = rootProject)
    }
    testRuntimeOnly(projectRuntimeJar(":kotlin-compiler"))

    testCompile(project(":compiler:backend"))
    testCompile(project(":compiler:cli"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val jar = runtimeJar {}

testsJar {}

dist(targetName = the<BasePluginConvention>().archivesBaseName.removePrefix("kotlin-") + ".jar")

projectTest {
    workingDir = rootDir
}
