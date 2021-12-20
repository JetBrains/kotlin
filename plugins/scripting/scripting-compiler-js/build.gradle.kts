description = "Kotlin Scripting Compiler JS Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:psi"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:backend.js"))
    compileOnly(project(":core:descriptors.runtime"))
    compileOnly(project(":compiler:ir.tree.impl"))
    compileOnly(project(":kotlin-reflect-api"))
    compile(project(":kotlin-scripting-common"))
    compile(project(":kotlin-scripting-js"))
    compile(project(":kotlin-util-klib"))
    compile(project(":kotlin-scripting-compiler"))
    compile(kotlinStdlib())
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    testCompile(project(":compiler:frontend"))
    testCompile(project(":compiler:plugin-api"))
    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:cli-common"))
    testCompile(project(":compiler:backend.js"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))

    testImplementation(intellijCoreDep()) { includeJars("intellij-core") }
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