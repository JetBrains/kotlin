/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.foreign

import org.jetbrains.org.objectweb.asm.tree.AnnotationNode
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.Metadata

internal fun readMetadata(annotationNode: AnnotationNode): KotlinClassMetadata? {
    val values = annotationNode.valuesByName.takeIf { it.isNotEmpty() } ?: return null

    val kind = values.getTyped<Int>("k") ?: return null
    val metadataVersion = values.getTypedArray("mv", ::IntArray) ?: return null
    val data1 = values.getTypedArray<String, Array<String>>("d1", ::Array) ?: return null
    val data2 = values.getTypedArray<String, Array<String>>("d2", ::Array) ?: return null
    val extraString = values.getTyped<String>("xs")
    val packageName = values.getTyped<String>("pn")
    val extraInt = values.getTyped<Int>("xi")

    val metadata = Metadata(kind, metadataVersion, data1, data2, extraString, packageName, extraInt)
    return KotlinClassMetadata.readStrict(metadata)
}

private inline fun <reified T : Any> Map<String, Any>.getTyped(name: String): T? {
    return this[name] as? T?
}

private inline fun <reified T : Any, reified R> Map<String, Any>.getTypedArray(name: String, factory: (Int, (Int) -> T) -> R): R? {
    val value = this.getTyped<List<Any?>>(name) ?: return null
    if (value.any { it !is T }) return null
    return factory(value.size) { index -> value[index] as T }
}

private val AnnotationNode.valuesByName: Map<String, Any>
    get() {
        val flatValues = this.values ?: return emptyMap()

        var index = 0
        val values = LinkedHashMap<String, Any>(flatValues.size / 2)

        while (index < flatValues.size) {
            val name = flatValues[index] as? String ?: continue
            val value = flatValues[index + 1] ?: continue
            values[name] = value
            index += 2
        }

        return values
    }