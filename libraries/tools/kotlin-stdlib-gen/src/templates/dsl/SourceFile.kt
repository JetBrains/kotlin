/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package templates

enum class SourceFile(jvmClassName: String? = null, val multifile: Boolean = true, val packageName: String? = null) {

    Arrays(packageName = "kotlin.collections"),
    UArrays(packageName = "kotlin.collections"),
    Collections(packageName = "kotlin.collections"),
    Sets(packageName = "kotlin.collections"),
    Maps(packageName = "kotlin.collections"),
    Sequences(packageName = "kotlin.sequences"),
    Ranges(packageName = "kotlin.ranges"),
    URanges(packageName = "kotlin.ranges"),
    Comparisons(packageName = "kotlin.comparisons"),
    Strings(packageName = "kotlin.text"),
    Misc(),
    ;

    val jvmClassName = jvmClassName ?: (name.capitalize() + "Kt")
}
