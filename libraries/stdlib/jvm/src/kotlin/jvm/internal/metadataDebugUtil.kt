/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/*
 * Kotlin debugger uses this function to fetch @kotlin.Metadata for a given class.
 * Originally, reading annotations from the debugger is not supported by JVM.
 *
 * The metadata is returned as a string in JSON format, where the `data1` field is
 * base64 encoded to keep the format valid.
 *
 * Returning metadata as a single string allows us to minimize the number of JDI calls from
 * the debugger side. Any JDWP communication can be very expensive when debugging remotely,
 * especially when debugging Android applications on a phone.
 */
@PublishedApi
@JvmName("getDebugMetadataAsJson")
internal fun Class<*>.getDebugMetadataAsJson(): String? {
    val metadata = getAnnotation(Metadata::class.java) ?: return null
    return with(metadata) {
        """
        {
            "kind": $kind,
            "metadataVersion": ${metadataVersion.toJson()},
            "data1": ${data1.toJsonBase64Encoded()},
            "data2": ${data2.toJson()},
            "extraString": ${extraString.toJson()},
            "packageName": ${packageName.toJson()},
            "extraInt": $extraInt
        }
        """.trimIndent()
    }
}

private fun IntArray.toJson(): String {
    return joinToString(separator = ",", prefix = "[", postfix = "]")
}

@OptIn(ExperimentalEncodingApi::class)
private fun Array<String>.toJsonBase64Encoded(): String {
    return joinToString(separator = ",", prefix = "[", postfix = "]") {
        Base64.Default.encode(it.toByteArray()).toJson()
    }
}

private fun Array<String>.toJson(): String {
    return joinToString(separator = ",", prefix = "[", postfix = "]") { it.toJson() }
}

private fun String.toJson(): String {
    return buildString {
        append('"')
        for (c in this@toJson) {
            if (c == '"') {
                append("\\\"")
            } else {
                append(c)
            }
        }
        append('"')
    }
}
