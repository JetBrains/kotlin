/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package foo

import kotlinx.serialization.Serializable

@Serializable
open class Base(
    val c: Int = 1,
    val b: String = "hello",
    val a: List<String> = listOf("a")
)

@Serializable
abstract class AbstractBase(
    val x: Int = 1,
    val y: Int = 2
) {
    abstract val nonSerializableProp: String
}
