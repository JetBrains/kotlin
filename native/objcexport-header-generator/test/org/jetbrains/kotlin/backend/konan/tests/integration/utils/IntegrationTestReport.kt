/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests.integration.utils

import org.jetbrains.kotlin.backend.konan.tests.integration.utils.IntegrationTestReport.Issue.*
import org.jetbrains.kotlin.native.interop.gen.getStringRepresentation
import org.jetbrains.kotlin.native.interop.indexer.*

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

        data class PropertySwiftName(
            val k1: String?, val k2: String?,
            val k1Source: ObjCClassOrProtocol,
            val k2Source: ObjCClassOrProtocol,
        ) : Issue()

        data class PropertyName(
            val k1: String,
            val k2: String,
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

        data class MethodReturnType(
            val k1Method: ObjCMethod, val k2Method: ObjCMethod?,
            val k1Type: Type?, val k2Type: Type?,
            val k1Source: ObjCClassOrProtocol,
            val k2Source: ObjCClassOrProtocol,
        ) : Issue()

        data class MethodParametersCount(
            val k1: Int, val k2: Int,
            val k1Source: ObjCClassOrProtocol,
            val k2Source: ObjCClassOrProtocol,
        ) : Issue()

        data class MethodParameterName(
            val k1: String, val k2: String,
            val k1Source: ObjCClassOrProtocol,
            val k2Source: ObjCClassOrProtocol,
        ) : Issue()

        data class MethodParameterType(
            val k1Method: ObjCMethod, val k2Method: ObjCMethod?,
            val k1Parameter: Parameter?, val k2Parameter: Parameter?,
            val k1Source: ObjCClassOrProtocol,
            val k2Source: ObjCClassOrProtocol,
        ) : Issue()

        data class PropertyType(
            val k1: ObjCProperty?, val k2: ObjCProperty?,
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
            result.add(MethodSelector(m1.selector, m2?.selector ?: "", k1, k2))
        }

        if (m1.swiftName != m2?.swiftName) {
            result.add(MethodSwiftName(m1.swiftName, m2?.swiftName ?: "", k1, k2))
        }

        val m1ParamsCount = m1.parameters.size
        val m2ParamsCount = m2?.parameters?.size ?: -1
        val k1ReturnType = m1.getReturnType(k1)
        val k2ReturnType = m2?.getReturnType(k2)
        val k1ReturnTypeAsString = k1ReturnType.getStringRepresentation()
        val k2ReturnTypeAsString = k2ReturnType?.getStringRepresentation() ?: "null"

        if (m1ParamsCount != m2ParamsCount) {
            result.add(MethodParametersCount(m1ParamsCount, m2ParamsCount, k1, k2))
        } else {
            m1.parameters.forEachIndexed { i1, p1 ->
                val p2 = m2?.parameters?.getOrNull(i1)
                val p1Name = p1.name ?: "null"
                val p2Name = p2?.name ?: "null"
                val p1Type = p1.type
                val p2Type = p2?.type
                val p1TypeAsString = p1Type.getStringRepresentation()
                val p2TypeAsString = p2Type?.getStringRepresentation() ?: "null"
                if (p1Name != p2Name) {
                    result.add(MethodParameterName(p1Name, p2Name, k1, k2))
                }
                if (p1TypeAsString != p2TypeAsString) {
                    result.add(MethodParameterType(m1, m2, p1, p2, k1, k2))
                }
            }
        }

        if (k1ReturnTypeAsString != k2ReturnTypeAsString) {
            result.add(MethodReturnType(m1, m2, k1ReturnType, k2ReturnType, k1, k2))
        }
    }

    return result
}

private fun compareProperties(k1: ObjCClassOrProtocol, k2: ObjCClassOrProtocol): List<IntegrationTestReport.Issue> {
    val result = mutableListOf<IntegrationTestReport.Issue>()

    k1.properties.forEachIndexed { i1, p1 ->
        val p2 = k2.properties.getOrNull(i1)
        val k1Type = p1.getType(k1)
        val k2Type = p2?.getType(k2)
        val k1TypeAsString = k1Type.getStringRepresentation()
        val k2TypeAsString = k2Type?.getStringRepresentation() ?: "null"

        if (p1.name != p2?.name) {
            result.add(PropertyName(p1.name, p2?.name ?: "", k1, k2))
        }

        if (p1.swiftName != p2?.swiftName) {
            result.add(PropertySwiftName(p1.swiftName, p2?.swiftName ?: "", k1, k2))
        }

        if (k1TypeAsString != k2TypeAsString) {
            result.add(PropertyType(p1, p2, k1, k2))
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
            result.add(DefinedInK1ButNotInK2(name, k1Container))
        } else {
            val k1Container = k1[name] ?: error("K1 container is null for $name")
            val k2Container = k2[name] ?: error("K2 container is null for $name")

            if (k1Container.swiftName != k2Container.swiftName) {
                result.add(
                    ClassOrInterfaceSwiftName(
                        k1Container.swiftName, k2Container.swiftName, k1Container, k2Container
                    )
                )
            }

            if (k1Container.name != k2Container.name) {
                result.add(
                    ClassOrInterfaceName(
                        k1Container.name, k2Container.name, k1Container, k2Container
                    )
                )
            }

            if (k1Container.methods.size != k2Container.methods.size) {
                result.add(
                    MethodsCount(
                        k1Container.methods.size, k2Container.methods.size, k1Container, k2Container
                    )
                )
            } else {
                result.addAll(compareMethods(k1Container, k2Container))
            }

            if (k1Container.properties.size != k2Container.properties.size) {
                result.add(
                    PropertiesCount(
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
            result.add(DefinedInK2ButNotInK1(name, k2Container))
        }
    }

    return result
}