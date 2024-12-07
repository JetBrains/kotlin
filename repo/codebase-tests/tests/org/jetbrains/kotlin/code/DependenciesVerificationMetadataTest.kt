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
}