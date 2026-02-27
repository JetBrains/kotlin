/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.jetbrains.kotlin.tooling.core.withClosure
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

/*
Code generation, parsing 'subsystems.yaml', generating the
'enum Subsystems' and all 'SubsystemInfo' implementations
 */

buildscript {
    repositories {
        mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
    }

    dependencies {
        classpath(libs.jackson.module.kotlin)
        classpath(libs.jackson.dataformat.yaml)
        classpath(kotlin("tooling-core", libs.versions.kotlin.`for`.gradle.plugins.compilation.get()))
    }
}

tasks.register("generateSubsystemSources") {
    val declaredSystems: RegularFile = project.isolated.rootProject.projectDirectory.file("../subsystems.yaml")
    val outputDir: Provider<Directory> = project.layout.buildDirectory.dir("genSrc")

    inputs.file(declaredSystems)
    outputs.dir(outputDir)

    doLast {
        val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
        val node = mapper.readTree(declaredSystems.asFile)
        val systems = node.properties().mapNotNull { (key, value) ->
            if (key.startsWith("$")) null else value.toDeclaredTestSystem(key)
        }

        val allSystems = systems.withClosure<DeclaredSubsystem> { system -> system.subsystems }

        outputDir.get().asFile.toPath().resolve("subsystems.kt").createParentDirectories().writeText(
            buildString {
                this += "// This file is generated automatically. DO NOT MODIFY IT MANUALLY"
                this += "// See 'codegen.gradle.kts'"

                this += "package org.jetbrains.kotlin.testFederation"
                this += "|"
                this += "|enum class Subsystem {"
                for (system in allSystems) {
                    this += "|    ${system.name},"
                }
                this += "|    Unknown,"
                this += "|}"
                this += "|"
                for (system in allSystems) {
                    this += "|internal object ${system.name}SubsystemInfo : SubsystemInfo {"
                    this += "|    override val home = \"${system.home}\""
                    this += "|    override val system = Subsystem.${system.name}"
                    this += "|    override val include: List<String> = listOf(${system.includes.joinToString { "\"$it\"" }})"
                    this += "|    override val exclude: List<String> = listOf(${system.excludes.joinToString { "\"$it\"" }})"
                    this += "|    override val subsystems: List<SubsystemInfo> by lazy { listOf(${system.subsystems.joinToString { "${it.name}SubsystemInfo" }}) }"
                    this += "|}"
                    this += "|"
                }

                this += "|"
                this += "|internal val SubsystemInfo.Companion.all: List<SubsystemInfo> get() = listOf("
                this += "|    " + allSystems.joinToString { "${it.name}SubsystemInfo" }
                this += "|)"
            }.trimMargin()
        )
    }
}

private fun JsonNode.toDeclaredTestSystem(key: String): DeclaredSubsystem {
    return DeclaredSubsystem(
        name = key,
        home = get("home").asText(),
        includes = get("include")?.valueStream()?.toList().orEmpty().map { it.asText() },
        excludes = get("excludes")?.valueStream()?.toList().orEmpty().map { it.asText() },
        subsystems = get("subsystems")?.properties().orEmpty().mapNotNull { (key, value) -> value.toDeclaredTestSystem(key) },
    )
}

private data class DeclaredSubsystem(
    val name: String,
    val home: String,
    val includes: List<String>,
    val excludes: List<String>,
    val subsystems: List<DeclaredSubsystem>,
)

private operator fun StringBuilder.plusAssign(s: String) {
    this.appendLine(s)
}
