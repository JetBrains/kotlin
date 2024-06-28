/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package generators.checkers

plugins {
    kotlin("jvm")
}

val generatorClasspath: Configuration by configurations.creating

dependencies {
    generatorClasspath(project(":compiler:fir:checkers:checkers-component-generator"))
}

val generateCheckersComponents by tasks.registering(JavaExec::class) {
    workingDir = rootDir
    classpath = generatorClasspath
    mainClass.set("org.jetbrains.kotlin.fir.checkers.generator.MainKt")
    systemProperties["line.separator"] = "\n"

    val generationRoot = layout.projectDirectory.dir("gen")
    args(project.name, generationRoot)
    outputs.dir(generationRoot)
}

sourceSets.named("main") {
    java.srcDirs(generateCheckersComponents)
}
