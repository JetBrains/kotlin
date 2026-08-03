/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.ir.generator.toolbox

import java.util.*

fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
fun String.decapitalize() = this.replaceFirstChar { it.lowercase(Locale.getDefault()) }
operator fun String.invoke(vararg args: String): String = this(args.toList())
operator fun String.invoke(args: List<String>? = null): String = "$this(${args?.joinToString() ?: ""})"
