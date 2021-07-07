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
    compileOnly(project(":compiler:backend.jvm:backend.jvm.entrypoint"))
    compileOnly(project(":kotlin-reflect-api"))
    compile(project(":kotlin-scripting-common"))
    compile(project(":kotlin-scripting-js"))
    compile(project(":kotlin-util-klib"))
    compile(project(":kotlin-scripting-jvm"))
    compile(project(":kotlin-scripting-compiler-impl"))
    compile(kotlinStdlib())
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    testCompile(project(":compiler:frontend"))
    testCompile(project(":compiler:plugin-api"))
    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:cli-common"))
    testCompile(project(":compiler:frontend.java"))
    testCompile(project(":compiler:backend.js"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))

    testImplementation(intellijCoreDep()) { includeJars("intellij-core") }
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
}

projectTest(taskName = "testWithIr", parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
    systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
    systemProperty("kotlin.script.test.base.compiler.arguments", "-Xuse-ir")
}

