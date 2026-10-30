/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer.internal

import org.jetbrains.kotlin.documentation.*
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaGetter

/**
 * Validates and ensures that all `KdElementId` instances within the `KdBundle` properly reference existing
 * elements in their respective `KdFragment`. Checks are performed for both `elements` and `embeddedElements`.
 *
 * This function inspects the `fragments` in the `KdBundle` and verifies:
 * - References in `elements` and `embeddedElements`.
 * - Correct mapping of IDs specified in `expectActualMappings`.
 *
 * If an element ID is found to reference a missing element or fragment during processing:
 * - An error is collected with a descriptive message indicating the missing ID, the fragment it was expected in,
 *   and the traversal path leading to the issue.
 *
 * The function performs recursive traversal where necessary, inspecting objects of various types such as
 * `Map`, `Iterable`, and other compound structures to ensure comprehensive validation of all relevant fields.
 * Reflection is used sparingly to automatically detect new `KdElementId` fields without requiring explicit handling.
 *
 * At the end of the validation, if unresolved element IDs are detected, an exception is thrown with a detailed list
 * of all missing references.
 *
 * Throws:
 * - `IllegalStateException`: If unresolved element IDs are found in the `KdBundle`.
 */
internal fun KdBundle.checkElementIds() {
    val fragmentsByName = fragments.associateBy { it.name }
    val missing = mutableListOf<String>()


    fun checkId(id: KdElementId, fragmentName: KdFragmentName, path: String) {
        // TODO kotlin.any is not supertype in Documentable model,
        // but Throwable.toString overrides Any.toString
        if (id.value.startsWith("kotlin/Any/") || id.value.startsWith("java.lang/Object/")) return

        val fragment = fragmentsByName[fragmentName]
        if (fragment == null || (id !in fragment.elements && id !in fragment.embeddedElements)) {
            missing += "$path: '${id.value}' does not exist in fragment '$fragmentName'"
        }
    }

    fragments.forEach { fragment ->
        val fragmentName = fragment.name
        listOf("elements" to fragment.elements, "embeddedElements" to fragment.embeddedElements).forEach { namedElements ->
            namedElements.second.forEach { entry ->
                val id = entry.key
                val element = entry.value
                val path = "$fragmentName.${namedElements.first}[${id.value}]"
                if (element is KdRefinedElement) {
                    checkId(id, element.fragmentName, "$path.refinedElement")
                } else {
                    visitElementIds(element, path) { referencedId, referencePath -> checkId(referencedId, fragmentName, referencePath) }
                }
            }
        }
    }
    for (index in expectActualMappings.indices) {
        val mapping = expectActualMappings[index]
        listOf("expectIds" to mapping.expectIds, "actualIds" to mapping.actualIds).forEach { namedIds ->
            namedIds.second.forEach { entry ->
                checkId(entry.value, entry.key, "expectActualMappings[$index].${namedIds.first}[${entry.key}]")
            }
        }
    }
    check(missing.isEmpty()) { "KDM contains unresolved element IDs:\n${missing.joinToString("\n")}" }
}

// Inspect model properties so newly added KdElementId fields are checked automatically as well.
internal fun visitElementIds(value: Any?, path: String, checkId: (KdElementId, String) -> Unit) {
    when (value) {
        null -> Unit
        is KdClassLikeType, is KdAnnotation, is KdThrows -> Unit // TODO support external links
        is KdElementId -> checkId(value, path)
        is Map<*, *> -> value.forEach { entry ->
            visitElementIds(entry.key, "$path.keys", checkId)
            visitElementIds(entry.value, "$path[${entry.key}]", checkId)
        }
        is Iterable<*> -> value.forEachIndexed { index, item -> visitElementIds(item, "$path[$index]", checkId) }
        else -> if (value !is Enum<*> && value.javaClass.name.substringBeforeLast('.') == "org.jetbrains.kotlin.documentation") {
            for (property in value::class.memberProperties) {
                // Reflection can box a null inline-class property as an ID with a null underlying value.
                if (property.returnType.isMarkedNullable && property.javaGetter?.invoke(value) == null) continue
                visitElementIds(property.getter.call(value), "$path.${property.name}", checkId)
            }
        }
    }
}
