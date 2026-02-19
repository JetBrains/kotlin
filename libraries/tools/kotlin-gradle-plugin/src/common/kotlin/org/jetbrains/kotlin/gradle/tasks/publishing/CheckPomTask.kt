/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.publishing

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

@CacheableTask
abstract class CheckPomTask internal constructor() : DefaultTask(), UsesKotlinToolingDiagnostics {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val pom: Property<File>

    @TaskAction
    protected fun execute() {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = builder.parse(pom.get())
        doc.documentElement.normalize()

        val projectTag = doc.getElementsByTagName("project").item(0)
        if (projectTag == null) {
            reportDiagnostic(
                KotlinToolingDiagnostics.PomMisconfigured(
                    "Couldn't parse pom.xml - <project> tag not found!",
                    "Please double check your publication configuration.",
                    "https://kotl.in/gradle-maven-publish-modifying-pom"
                )
            )
            return
        }

        val missingTags = requiredPomTags.toMutableSet()
        val xPath = XPathFactory.newInstance().newXPath()
        requiredPomTags.forEach { requiredTag ->
            val node = xPath.compile("./$requiredTag").evaluate(projectTag, XPathConstants.NODE)
            if (node != null) {
                missingTags -= requiredTag
            }
        }
        if (missingTags.isNotEmpty()) {
            reportDiagnostic(
                KotlinToolingDiagnostics.PomMisconfigured(
                    buildString {
                        appendLine("Missing tags in POM:")
                        missingTags.forEach { missingTag ->
                            appendLine("* ${missingTag.split("/").joinToString(" - ") { "<${it.substringBefore("[")}>" }}")
                        }
                        appendLine("These tags are required for publication to the Maven Central Repository as described here: https://kotl.in/sonatype-required-pom-metadata")
                    },
                    "If you're using the 'maven-publish' plugin please refer to the documentation on how to set the required POM values.",
                    "https://kotl.in/gradle-maven-publish-modifying-pom"
                )
            )
            return
        }
    }

    private companion object {
        val requiredPomTags = listOf(
            "groupId",
            "artifactId",
            "version",
            "name",
            "description",
            "url",
            "licenses",
            "licenses/license[1]",
            "licenses/license[1]/name",
            "licenses/license[1]/url",
            "developers",
            "developers/developer[1]",
            "developers/developer[1]/name",
            "developers/developer[1]/email",
            "developers/developer[1]/organization",
            "developers/developer[1]/organizationUrl",
            "scm",
            "scm/connection",
            "scm/developerConnection",
            "scm/url"
        )
    }
}