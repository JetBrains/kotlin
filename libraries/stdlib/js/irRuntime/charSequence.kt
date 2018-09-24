/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

internal annotation class DoNotIntrinsify

@PublishedApi
@DoNotIntrinsify
internal fun charSequenceGet(a: CharSequence, index: Int): Char {
    return  if (a is String) {
        Char(a.asDynamic().charCodeAt(index).unsafeCast<Int>())
    } else {
        a[index]
    }
}

@PublishedApi
@DoNotIntrinsify
internal fun charSequenceLength(a: CharSequence): Int {
    return if (a is String) {
        a.asDynamic().length.unsafeCast<Int>()
    } else {
        a.length
    }
}

@PublishedApi
@DoNotIntrinsify
internal fun charSequenceSubSequence(a: CharSequence, startIndex: Int, endIndex: Int): CharSequence {
    return if (a is String) {
        a.asDynamic().substring(startIndex, endIndex).unsafeCast<String>()
    } else {
        a.subSequence(startIndex, endIndex)
    }
}
