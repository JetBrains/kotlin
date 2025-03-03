/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File

fun parsePom(file: File): PublishedPom = PublishedPom(
    javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(file)
)

class PublishedPom(
    private val document: Document,
) {
    val selfReference: MavenModule
        get() = document.documentElement.mavenModule()

    fun dependencies(): List<MavenModule> = document.documentElement
        .childrenWithTag("dependencies").single()
        .childrenWithTag("dependency")
        .map {
            it.mavenModule()
        }

    fun dependencyManagementConstraints(): List<MavenModule> = document.documentElement
        .childrenWithTag("dependencyManagement").single()
        .childrenWithTag("dependencies").single()
        .childrenWithTag("dependency")
        .map {
            it.mavenModule()
        }

    private fun Element.mavenModule(): MavenModule = MavenModule(
        childrenWithTag("groupId").singleOrNull()?.textContent,
        childrenWithTag("artifactId").singleOrNull()?.textContent,
        childrenWithTag("version").singleOrNull()?.textContent,
        childrenWithTag("scope").singleOrNull()?.textContent
    )

    private fun Element.childrenWithTag(name: String): List<Element> {
        return (0..<childNodes.length).mapNotNull { childNodes.item(it) as? Element }.filter { it.tagName == name }
    }
}

data class MavenModule(
    val groupId: String?,
    val artifactId: String?,
    val version: String?,
    val scope: String?,
)
