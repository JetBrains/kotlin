/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

// Copied from libraries/stdlib/js/src/kotlin/collections/utils.kt
// Current inliner doesn't rename symbols inside `js` fun
@Suppress("UNUSED_PARAMETER")
internal fun deleteProperty(obj: Any, property: Any) {
    js("delete obj[property]")
}
