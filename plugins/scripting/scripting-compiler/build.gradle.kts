description = "Kotlin Scripting Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:psi"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:backend.js"))
    compileOnly(project(":core:descriptors.runtime"))
    compileOnly(project(":compiler:ir.tree.impl"))
    compileOnly(project(":compiler:backend.jvm.entrypoint"))
    compileOnly(project(":kotlin-reflect-api"))
    api(project(":kotlin-scripting-common"))
    api(project(":kotlin-scripting-js"))
    api(project(":kotlin-util-klib"))
    api(project(":kotlin-scripting-jvm"))
    api(project(":kotlin-scripting-compiler-impl"))
    api(kotlinStdlib())
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    testApi(project(":compiler:frontend"))
    testApi(project(":compiler:plugin-api"))
    testApi(project(":compiler:util"))
    testApi(project(":compiler:cli"))
    testApi(project(":compiler:cli-common"))
    testApi(project(":compiler:frontend.java"))
    testApi(project(":compiler:backend.js"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(commonDep("junit:junit"))

    testImplementation(intellijCoreDep()) { includeJars("intellij-core") }
    testImplementation(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core"))
    testRuntimeOnly(intellijDep()) { includeJars("jps-model", "jna") }

    testImplementation(project(":kotlin-reflect"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs - "-progressive" + "-Xskip-metadata-version-check"
    }
}

publish()

runtimeJar()
sourcesJar()
javadocJar()

testsJar()

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
    systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
    systemProperty("kotlin.script.test.base.compiler.arguments", "-Xuse-old-backend")
}

projectTest(taskName = "testWithIr", parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
    systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
    systemProperty("kotlin.script.test.base.compiler.arguments", "-Xuse-ir")
}

