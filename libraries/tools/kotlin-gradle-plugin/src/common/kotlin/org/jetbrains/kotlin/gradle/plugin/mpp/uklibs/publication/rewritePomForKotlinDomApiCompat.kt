/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication

import groovy.util.Node
import groovy.util.NodeList
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import kotlin.collections.filterIsInstance
import kotlin.collections.orEmpty

/**
 * This workaround fixes problem when UKlib producer with JVM and JS gets consumed on Maven site
 * and fails to resolve dom-api-compat dependency.
 * Rewriting it to <type>pom</type> allows it to be consumed correctly, even without stub-jar present.
 *
 * See KT-78751
 */
internal fun Project.rewritePomForKotlinDomApiCompat() {
    plugins.withId("maven-publish") {
        project.extensions.configure(PublishingExtension::class.java) { publishing ->
            publishing.publications.withType(MavenPublication::class.java).all { publication ->
                publication.pom.withXml(XmlProvider::addPomTypeToKotlinDomApiCompatDependency)
            }
        }
    }
}

private fun XmlProvider.addPomTypeToKotlinDomApiCompatDependency() {
    val dependenciesNode = (asNode().get("dependencies") as NodeList).filterIsInstance<Node>().singleOrNull() ?: return
    val dependencyNodes = (dependenciesNode.get("dependency") as? NodeList).orEmpty().filterIsInstance<Node>()
    dependencyNodes.forEach { dependencyNode ->
        val groupId = (dependencyNode.get("groupId") as? NodeList)?.text() ?: return@forEach
        val artifactId = (dependencyNode.get("artifactId") as? NodeList)?.text() ?: return@forEach
        val type = (dependencyNode.get("type") as? NodeList) ?: return@forEach
        if (groupId == "org.jetbrains.kotlin" &&
            artifactId == "kotlin-dom-api-compat" &&
            type.size == 0
        ) {
            dependencyNode.appendNode("type", "pom")
        }
    }
}