/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package example.cinterop.project

import library.cinterop.project.*

fun libraryAnswer(): Int {
    return publishedAnswer() + projectAnswer()
}

fun selfCalculatedAnswer(): Int {
    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    return getNumber() * 2 + getAnotherNumber() * 2
}
