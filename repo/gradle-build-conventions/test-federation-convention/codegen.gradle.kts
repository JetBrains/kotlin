/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

/*
Code generation, parsing 'domains.yaml', generating the
'enum Domain' and all 'DomainInfo' implementations
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

tasks.register("generateDomainSources") {
    val declaredDomains: RegularFile = project.isolated.rootProject.projectDirectory.file("../domains.yaml")
    val outputDir: Directory = project.layout.projectDirectory.dir("src/main/generated")

    inputs.file(declaredDomains)
    outputs.dir(outputDir)

    doLast {
        val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
        val node = mapper.readTree(declaredDomains.asFile)
        val domains = node.properties().mapNotNull { (key, value) ->
            if (key.startsWith("$")) null else value.toDomainDTO(key)
        }


        outputDir.asFile.toPath().resolve("domains.kt").createParentDirectories().writeText(
            buildString {
                this += "// This file is generated automatically. DO NOT MODIFY IT MANUALLY"
                this += "// See 'codegen.gradle.kts'"

                this += "package org.jetbrains.kotlin.testFederation"
                this += "|"
                this += "|enum class Domain {"
                for (domain in domains) {
                    this += "|    ${domain.name},"
                }
                this += "|    ;"
                this += "|"
                this += "|    companion object"
                this += "|}"
                this += "|"
                for (domain in domains) {
                    this += "|internal object ${domain.name}DomainInfo : DomainInfo {"
                    this += "|    override val home = \"${domain.home}\""
                    this += "|    override val domain = Domain.${domain.name}"
                    this += "|    override val include: List<String> = listOf(${domain.includes.joinToString { "\"$it\"" }})"
                    this += "|    override val exclude: List<String> = listOf(${domain.excludes.joinToString { "\"$it\"" }})"
                    this += "|    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(${domain.fullyAffectedBy.joinToString { "${it}DomainInfo" }}) }"
                    this += "|}"
                    this += "|"
                }

                this += "|"
                this += "|internal val allDomainInfos: List<DomainInfo> by lazy {"
                this += "|    listOf("
                for (domain in domains) {
                    this += "|        ${domain.name}DomainInfo,"
                }
                this += "|    )"
                this += "|}"
            }.trimMargin()
        )
    }
}

private fun JsonNode.toDomainDTO(key: String): DomainDTO {
    return DomainDTO(
        name = key,
        home = get("home").asText(),
        includes = get("include")?.valueStream()?.toList().orEmpty().map { it.asText() },
        excludes = get("exclude")?.valueStream()?.toList().orEmpty().map { it.asText() },
        fullyAffectedBy = get("fullyAffectedBy")?.valueStream()?.toList().orEmpty().map { it.asText() },
    )
}

private data class DomainDTO(
    val name: String,
    val home: String,
    val includes: List<String>,
    val excludes: List<String>,
    val fullyAffectedBy: List<String>,
)

private operator fun StringBuilder.plusAssign(s: String) {
    this.appendLine(s)
}
