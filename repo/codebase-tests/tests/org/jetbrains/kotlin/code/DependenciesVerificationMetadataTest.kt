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
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import java.io.File
import kotlin.test.fail


class DependenciesVerificationMetadataTest {
    @JacksonXmlRootElement(localName = "verification-metadata")
    private data class VerificationMetadata(
        @field:JacksonXmlElementWrapper(localName = "components")
        val components: List<Component> = listOf(),
        @field:JacksonXmlProperty
        val configuration: Configuration,
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

    private data class Configuration(
        /*  `@JacksonXmlProperty` is used here instead of `@JacksonXmlElementWrapper`
         *  because even with `localName = "trusted-artifacts"` defined in `@JacksonXmlElementWrapper`
         *  `trustedArtifacts` property is expected:
         *    "Could not find creator property with name 'trusted-artifacts' (known Creator properties: [trustedArtifacts])"
         *  This could be related to https://github.com/FasterXML/jackson-module-kotlin/issues/153
         *  Not using the wrapper breaks serialization, but the tests only read the verification metadata file.
         */
        @field:JacksonXmlProperty(localName = "trusted-artifacts")
        val trustedArtifacts: List<TrustedArtifact> = listOf(),
    )

    private data class Artifact(
        @field:JacksonXmlProperty(isAttribute = true)
        val name: String,
        val sha256: Hash?,
    )

    private data class Hash(@field:JacksonXmlProperty(isAttribute = true) val value: String)

    private data class TrustedArtifact(
        @field:JacksonXmlProperty(isAttribute = true)
        val group: String? = null,
        @field:JacksonXmlProperty(isAttribute = true)
        val name: String? = null,
        @field:JacksonXmlProperty(isAttribute = true)
        val version: String? = null,
        @field:JacksonXmlProperty(isAttribute = true)
        val file: String? = null,
        @field:JacksonXmlProperty(isAttribute = true)
        val regex: String? = null,
        @field:JacksonXmlProperty(isAttribute = true)
        val reason: String? = null,
    )

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
                if (artifact.sha256 == null) {
                    fails.add("${artifact.name} - sha256 is missing")
                }
            }
        }

        assertTrue(fails.isEmpty(), fails.toString())
    }

    @Test
    fun noNewTrustedArtifacts() {
        val mapper = XmlMapper()
            .apply {
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                registerKotlinModule()
            }
        val verificationMetadata = mapper.readValue<VerificationMetadata>(File("gradle/verification-metadata.xml"))

        val trustedArtifactsExceptions = listOf(
            TrustedArtifact(file = ".*-javadoc[.]jar", regex = "true"),
            TrustedArtifact(file = ".*-sources[.]jar", regex = "true"),
            TrustedArtifact(file = ".*[.]module", regex = "true"),
            TrustedArtifact(file = ".*[.]pom", regex = "true"),
            TrustedArtifact(group = "gradle", name = "gradle", file = "gradle-.*-src[.]zip", regex = "true"),
        )
        val trustedArtifacts = verificationMetadata.configuration.trustedArtifacts
        val unexpectedTrustedArtifacts = trustedArtifacts.filter { it !in trustedArtifactsExceptions }
        val unneededExceptions = trustedArtifactsExceptions.filter { it !in trustedArtifacts }

        val failMessage = buildString {
            if (unexpectedTrustedArtifacts.isNotEmpty()) {
                appendLine("Unexpected trusted artifacts:")
                unexpectedTrustedArtifacts.forEach {
                    appendLine("    $it")
                }
            }
            if (unneededExceptions.isNotEmpty()) {
                appendLine("Unneeded exceptions, remove them from the test:")
                unneededExceptions.forEach {
                    appendLine("    $it")
                }
            }
        }
        if (failMessage.isNotEmpty()) {
            fail(failMessage)
        }
    }
}
