/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

@Deprecated(message = "Use `definedExternally` instead", level = DeprecationLevel.ERROR, replaceWith = ReplaceWith("definedExternally"))
public external val noImpl: Nothing

@Deprecated("Use toInt() instead.", ReplaceWith("s.toInt()"), level = DeprecationLevel.ERROR)
public external fun parseInt(s: String): Int

@Deprecated("Use toInt(radix) instead.", ReplaceWith("s.toInt(radix)"), level = DeprecationLevel.ERROR)
public external fun parseInt(s: String, radix: Int = definedExternally): Int

@Deprecated("Use toDouble() instead.", ReplaceWith("s.toDouble()"), level = DeprecationLevel.ERROR)
public external fun parseFloat(s: String, radix: Int = definedExternally): Double
