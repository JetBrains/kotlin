/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.mapping.bytecode

internal class JvmDescriptor private constructor(
    val parameters: List<String>,
    val returnType: String
) {
    companion object {
        internal fun fromString(descriptor: String): JvmDescriptor {
            val parameterString = descriptor.takeWhile { it != ')' }.dropWhile { it == '(' }
            val parameters = buildList {
                var i = 0
                while (i < parameterString.length) {
                    val start = i
                    var current = parameterString[i]
                    while (current == '[') {
                        i++
                        current = parameterString[i]
                    }
                    val end = if (current == 'L') {
                        parameterString.indexOf(';', i) + 1
                    } else {
                        i + 1
                    }
                    add(parameterString.substring(start, end))
                    i = end
                }
            }
            val returnType = descriptor.takeLastWhile { it != ')' }
            return JvmDescriptor(parameters, returnType)
        }
    }
}