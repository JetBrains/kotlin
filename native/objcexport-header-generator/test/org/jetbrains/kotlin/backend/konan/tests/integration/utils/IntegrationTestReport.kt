/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests.integration.utils

import org.jetbrains.kotlin.native.interop.indexer.IndexerResult
import org.jetbrains.kotlin.native.interop.indexer.ObjCClassOrProtocol

internal data class IntegrationTestReport(
    val name: String,
    val issues: List<Issue>,
) {
    sealed class Issue {

        data class DefinedInK2ButNotInK1(
            val k2: String,
            val k1Source: ObjCClassOrProtocol,
        ) : Issue()

        data class DefinedInK1ButNotInK2(
            val k1: String,
            val k1Source: ObjCClassOrProtocol,
        ) : Issue()

        data class ClassOrInterfaceName(
            val k1: String,
            val k2: String,
            val k1Source: ObjCClassOrProtocol,
            val k2Source: ObjCClassOrProtocol,
        ) : Issue()

        data class ClassOrInterfaceSwiftName(
            val k1: String?, val k2: String?,
            val k1Source: ObjCClassOrProtocol,
            val k2Source: ObjCClassOrProtocol,
        ) : Issue()

        data class MethodsCount(
            val k1: Int, val k2: Int,
            val k1Source: ObjCClassOrProtocol,
            val k2Source: ObjCClassOrProtocol,
        ) : Issue()

        data class PropertiesCount(
            val k1: Int, val k2: Int,
            val k1Source: ObjCClassOrProtocol,
            val k2Source: ObjCClassOrProtocol,
        ) : Issue()

        data class MethodSelector(
            val k1: String,
            val k2: String,
            val k1Source: ObjCClassOrProtocol,
            val k2Source: ObjCClassOrProtocol,
        ) : Issue()

        data class MethodSwiftName(
            val k1: String?, val k2: String?,
            val k1Source: ObjCClassOrProtocol,
            val k2Source: ObjCClassOrProtocol,
        ) : Issue()

        data class FailedK1Compilation(
            val message: String?,
            val header: String,
            val error: Throwable,
        ) : Issue()

        data class FailedK2CompilationK2(
            val message: String?,
            val header: String,
            val error: Throwable,
        ) : Issue()
    }

    val hasIssues: Boolean
        get() {
            return issues.isNotEmpty()
        }
}

internal fun compareProtocolsOrClasses(
    ik1: IndexerResult,
    ik2: IndexerResult,
): List<IntegrationTestReport.Issue> {
    val k1Classes = ik1.index.objCClasses.associateBy { it.name }
    val k2Classes = ik2.index.objCClasses.associateBy { it.name }
    val k1Protocols = ik1.index.objCProtocols.associateBy { it.name }
    val k2Protocols = ik2.index.objCProtocols.associateBy { it.name }

    return compareProtocolsOrClasses(k1Classes, k2Classes) + compareProtocolsOrClasses(k1Protocols, k2Protocols)
}

private fun compareMethods(k1: ObjCClassOrProtocol, k2: ObjCClassOrProtocol): List<IntegrationTestReport.Issue> {
    val result = mutableListOf<IntegrationTestReport.Issue>()

    k1.methods.forEachIndexed { i1, m1 ->
        val m2 = k2.methods.getOrNull(i1)

        if (m1.selector != m2?.selector) {
            result.add(IntegrationTestReport.Issue.MethodSelector(m1.selector, m2?.selector ?: "", k1, k2))
        }

        if (m1.swiftName != m2?.swiftName) {
            result.add(IntegrationTestReport.Issue.MethodSwiftName(m1.swiftName, m2?.swiftName ?: "", k1, k2))
        }
    }

    return result
}

private fun compareProperties(k1: ObjCClassOrProtocol, k2: ObjCClassOrProtocol): List<IntegrationTestReport.Issue> {
    val result = mutableListOf<IntegrationTestReport.Issue>()

    k1.properties.forEachIndexed { i1, p1 ->
        val p2 = k2.properties.getOrNull(i1)

        if (p1.name != p2?.name) {
            result.add(IntegrationTestReport.Issue.MethodSelector(p1.name, p2?.name ?: "", k1, k2))
        }

        if (p1.swiftName != p2?.swiftName) {
            result.add(IntegrationTestReport.Issue.MethodSwiftName(p1.swiftName, p2?.swiftName ?: "", k1, k2))
        }
    }

    return result
}

private fun compareProtocolsOrClasses(
    k1: Map<String, ObjCClassOrProtocol>,
    k2: Map<String, ObjCClassOrProtocol>,
): List<IntegrationTestReport.Issue> {

    val result = mutableListOf<IntegrationTestReport.Issue>()

    k1.forEach { (name, k1Container) ->

        if (!k2.keys.contains(name)) {
            result.add(IntegrationTestReport.Issue.DefinedInK1ButNotInK2(name, k1Container))
        } else {
            val k1Container = k1[name] ?: error("K1 container is null for $name")
            val k2Container = k2[name] ?: error("K2 container is null for $name")

            if (k1Container.swiftName != k2Container.swiftName) {
                result.add(
                    IntegrationTestReport.Issue.ClassOrInterfaceSwiftName(
                        k1Container.swiftName, k2Container.swiftName, k1Container, k2Container
                    )
                )
            }

            if (k1Container.name != k2Container.name) {
                result.add(
                    IntegrationTestReport.Issue.ClassOrInterfaceName(
                        k1Container.name, k2Container.name, k1Container, k2Container
                    )
                )
            }

            if (k1Container.methods.size != k2Container.methods.size) {
                result.add(
                    IntegrationTestReport.Issue.MethodsCount(
                        k1Container.methods.size, k2Container.methods.size, k1Container, k2Container
                    )
                )
            } else {
                result.addAll(compareMethods(k1Container, k2Container))
            }

            if (k1Container.properties.size != k2Container.properties.size) {
                result.add(
                    IntegrationTestReport.Issue.PropertiesCount(
                        k1Container.properties.size, k2Container.properties.size, k1Container, k2Container
                    )
                )
            } else {
                result.addAll(compareProperties(k1Container, k2Container))
            }

        }
    }

    k2.forEach { (name, k2Container) ->
        if (!k1.keys.contains(name)) {
            result.add(IntegrationTestReport.Issue.DefinedInK2ButNotInK1(name, k2Container))
        }
    }

    return result
}