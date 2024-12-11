/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication

import groovy.util.Node
import groovy.util.NodeList
import org.gradle.api.XmlProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsageContext

// FIXME: Project dependencies?
// FIXME: Look at the (unresolved?) dependencies of all configurations that contribute to the POM (UsageComponents?) and set compile/runtime accordingly
internal class UklibPomDependenciesRewriter {
    data class DependencyGA(
        val group: String?,
        val artifact: String,
    )

    data class TargetDep(
        val ga: DependencyGA,
        val scope: KotlinUsageContext.MavenScope?,
    )

    fun makeAllDependenciesCompile(
        pomXml: XmlProvider,
        mapping: Map<DependencyGA, TargetDep>,
    ) {
        val dependenciesNode = (pomXml.asNode().get("dependencies") as NodeList).filterIsInstance<Node>().singleOrNull() ?: return
        val dependencyNodes = (dependenciesNode.get("dependency") as? NodeList).orEmpty().filterIsInstance<Node>()

        dependencyNodes.forEach { dependencyNode ->
//            fun Node.getSingleChildValueOrNull(childName: String): String? =
//                ((get(childName) as NodeList?)?.singleOrNull() as Node?)?.text()
            val group = ((dependencyNode.get("groupId") as NodeList).singleOrNull() as Node?)?.text() ?: return@forEach
            val artifact = ((dependencyNode.get("artifactId") as NodeList).singleOrNull() as Node?)?.text() ?: return@forEach
            val scope = ((dependencyNode.get("scope") as NodeList).singleOrNull() as Node?)

            // Leave if it's already compile
            if (scope?.text() == "compile") return@forEach
            mapping[DependencyGA(group, artifact)]?.let {
                when (it.scope) {
                    KotlinUsageContext.MavenScope.COMPILE -> scope?.setValue("compile")
                    KotlinUsageContext.MavenScope.RUNTIME -> scope?.setValue("runtime")
                }
            }
            // When do we actually need to map dependencies?
        }
    }
}