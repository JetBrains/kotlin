/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.junit.Test
import java.io.File


class DependenciesVerificationMetadataTest {
    @JacksonXmlRootElement(localName = "verification-metadata")
    private data class VerificationMetadata(
        @field:JacksonXmlElementWrapper(localName = "components")
        val components: List<Component> = listOf()
    )

    private data class Component(
        @field:JacksonXmlProperty(isAttribute = true)
        val group: String,
        @field:JacksonXmlProperty(isAttribute = true)
        val name: String,
        @field:JacksonXmlProperty(isAttribute = true)
        val version: String,
        @field:JacksonXmlElementWrapper(useWrapping = false)
        val artifact: List<Artifact>,
    )

    private data class Artifact(
        @field:JacksonXmlProperty(isAttribute = true)
        val name: String,
        val md5: Hash?,
        val sha256: Hash?
    )

    private data class Hash(@field:JacksonXmlProperty(isAttribute = true) val value: String)

    @Test
    fun dependenciesHasValidHashes() {
        val mapper = XmlMapper()
            .apply {
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                registerKotlinModule()
            }
        val verificationMetadata = mapper.readValue<VerificationMetadata>(File("gradle/verification-metadata.xml"))
        val fails = mutableListOf<String>()

        verificationMetadata.components.forEach { component ->
            component.artifact.forEach { artifact ->
                if (artifact.md5 == null) {
                    fails.add("${artifact.name} - md5 is missing")
                }
                if (artifact.sha256 == null) {
                    fails.add("${artifact.name} - sha256 is missing")
                }
            }
        }

        KtUsefulTestCase.assertEmpty(fails)
    }

    @Test
    fun canBeDeletedDependencies() {
        // ./gradlew -i -Pkotlin.native.enabled=true -Pteamcity=true allDependencies > dependencies.txt
        val dependenciesFile = File("dependencies.txt")

        // Check with `./gradlew -i --write-verification-metadata sha256,md5 -Pkotlin.native.enabled=true -Pteamcity=true --dry-run help dokkaGradle70Javadoc`
        // cp gradle/verification-metadata.dryrun.xml gradle.verification-metadata.xml

        // Also some tasks give additional dependencies :(
        // :examples:scripting-jvm-maven-deps:compileKotlin - adds also trust for 1.5.0 stdlib
        val additionalUsed = listOf(
            // For kotlinYarnSetup
            "com.yarnpkg:yarn:1.22.17",

            "google:emulator-darwin:5264690",
            "google:emulator-linux:5264690",
            "google:emulator-windows:5264690",
            "google:sdk-tools-darwin:4333796",
            "google:sdk-tools-linux:4333796",
            "google:sdk-tools-windows:4333796",
            // Need for dokka. -M option doesn't insert them
            "com.soywiz.korlibs.korte:korte-jvm:2.7.0",
            "google:x86:19",
            "org.jetbrains.dokka:javadoc-plugin:1.7.0",
            "org.jetbrains.dokka:kotlin-as-java-plugin:1.7.0",
            // Not placed during with -M option, needed in :kotlin-test:kotlin-test-js-ir:kotlin-test-js-ir-it
            "org.nodejs:node:16.13.0",
            "org.nodejs:node:18.12.1",
            // Probably needed for buildSrc and for Gradle plugins
            "com.github.ajalt:clikt:2.1.0",
            "com.github.node-gradle:gradle-node-plugin:3.0.1",
            "com.github.node-gradle:gradle-node-plugin:3.2.1",
            "com.gradle.publish:plugin-publish-plugin:1.0.0",
            "com.gradle:common-custom-user-data-gradle-plugin:1.8.1",
            "com.jakewharton.android.repackaged:dalvik-dx:9.0.0_r3",
            "com.jakewharton.dex:dex-member-list:4.1.1",
            "com.jetbrains.intellij.idea:ideaIC:203.8084.24",
            "com.jetbrains.intellij.idea:intellij-core:203.8084.24",
            "com.jetbrains.intellij.idea:jps-standalone:203.8084.24",
            "gradle.plugin.org.gradle.crypto:checksum:1.2.0",
            "gradle.plugin.org.jetbrains.gradle.plugin.idea-ext:gradle-idea-ext:1.0.1",
            "org.gradle.kotlin:gradle-kotlin-dsl-plugins:2.1.7",
            "org.gradle:test-retry-gradle-plugin:1.2.0",
            "org.jetbrains.dokka:dokka-gradle-plugin:1.7.0",
            "org.jetbrains.kotlin:kotlin-android-extensions:1.5.31",
            "org.jetbrains.kotlin:kotlin-annotation-processing-gradle:1.5.31",
            "org.jetbrains.kotlin:kotlin-build-common:1.5.31",
            "org.jetbrains.kotlin:kotlin-compiler-runner:1.5.31",
            "org.jetbrains.kotlin:kotlin-daemon-client:1.5.31",
            "org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.31",
            "org.jetbrains.kotlin:kotlin-klib-commonizer-api:1.5.31",
            "org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:1.5.31",
            "org.jetbrains.kotlin:kotlin-scripting-compiler-impl-embeddable:1.5.31",
            "org.jetbrains.kotlin:kotlin-scripting-jvm:1.5.31",
            "org.jetbrains.kotlin:kotlin-tooling-metadata:1.5.31",
            "org.jetbrains.kotlin:kotlin-util-klib-metadata:1.4.30",
            "org.jetbrains.kotlinx:kotlinx-benchmark-plugin:0.3.1",
            "org.jetbrains.kotlinx:kotlinx-metadata-klib:0.0.1-dev-10"
        )

        val used: MutableMap<String, MutableMap<String, MutableSet<String>>> = mutableMapOf()
        val badLines: MutableList<String> = mutableListOf()

        dependenciesFile.useLines { lines ->
            lines.filter { it.contains("---") }
                .filter { !it.contains("project ") }
                .map { it.substringAfterLast("---") }
                .filterNot { it.contains("(*)") || it.contains("(n)") } // Filter already listed and not meant to be resolved
                .map { it.substringBeforeLast("(") }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { line ->
                    val substitutionSplit = line.split("->")
                    val mainSplit = substitutionSplit[0].trim().split(":")
                    if (mainSplit.size == 3 || (mainSplit.size == 2 && substitutionSplit.size == 2)) {
                        val group = mainSplit[0]
                        val name = mainSplit[1]
                        val version = if (substitutionSplit.size > 1) substitutionSplit[1].trim() else mainSplit[2]

                        ((used.getOrPut(group) { mutableMapOf() }).getOrPut(name) { mutableSetOf() }).add(version)
                    } else {
                        badLines.add(line)
                    }
                }

            for (exceptionDependency in additionalUsed) {
                val split = exceptionDependency.split(":")
                val group = split[0]
                val name = split[1]
                val version = split[2]

                ((used.getOrPut(group) { mutableMapOf() }).getOrPut(name) { mutableSetOf() }).add(version)
            }

            KtUsefulTestCase.assertEmpty(badLines)

            val mapper = XmlMapper()
                .apply {
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    registerKotlinModule()
                }
            val verificationMetadata = mapper.readValue<VerificationMetadata>(File("gradle/verification-metadata.xml"))
            val shouldBeDeletedComponents = ArrayList<Component>()
            for (component in verificationMetadata.components) {
                if (used[component.group]?.get(component.name)?.contains(component.version) == null) {
                    shouldBeDeletedComponents.add(component)
                }
            }

            /*
            val shouldBeDeletedLines = shouldBeDeletedComponents.map { component ->
                with(component) {
                    "<component group=\"${group}\" name=\"${name}\" version=\"${version}\">"
                }
            }.toSet()

            val verificationMetadataFile = File("gradle/verification-metadata.xml")
            var inDropLine = false
            val modifiedFileLines = verificationMetadataFile.readLines().filter { line ->
                val trimmed = line.trim()
                if (trimmed in shouldBeDeletedLines) {
                    inDropLine = true
                }

                var result = inDropLine

                if (trimmed == "</component>") {
                    inDropLine = false
                }

                !result
            }

            verificationMetadataFile.writeText(modifiedFileLines.joinToString("\n"))
            */

            KtUsefulTestCase.assertEmpty(shouldBeDeletedComponents.map { component ->
                with(component) {
                    "$group:$name:$version"
                }
            }.toSet())
        }
    }
}