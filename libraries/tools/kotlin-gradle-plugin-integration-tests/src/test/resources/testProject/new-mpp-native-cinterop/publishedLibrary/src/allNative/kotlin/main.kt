/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package example.cinterop.published

import example.cinterop.published.stdio.*
import com.example.lib.*

fun publishedPrint(str: String) {
    printf(str + '\n')
    println(id(42))
}
