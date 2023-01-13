/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DEPRECATION")
package kotlinx.metadata.internal.extensions

import kotlinx.metadata.KmExtensionType
import kotlinx.metadata.KmExtensionVisitor

internal fun <T : KmExtensionVisitor> applySingleExtension(type: KmExtensionType, block: MetadataExtensions.() -> T?): T? {
    var result: T? = null
    for (extension in MetadataExtensions.INSTANCES) {
        val current = block(extension) ?: continue
        if (result != null) {
            throw IllegalStateException("Multiple extensions handle the same extension type: $type")
        }
        result = current
    }
    return result
}

internal fun <N : KmExtension<*>> Collection<N>.singleOfType(type: KmExtensionType): N {
    var result: N? = null
    for (node in this) {
        if (node.type != type) continue
        if (result != null) {
            throw IllegalStateException("Multiple extensions handle the same extension type: $type")
        }
        result = node
    }
    if (result == null) {
        throw IllegalStateException("No extensions handle the extension type: $type")
    }
    return result
}
