/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("EXPERIMENTAL_API_USAGE_ERROR")

package kotlinx.cinterop

import kotlinx.cinterop.CStructVar
import kotlin.native.internal.Cleaner
import kotlin.native.internal.createCleaner

interface CPlusPlusClass {
    fun __destroy__()
}

abstract class ManagedType<T> (val cpp: T)
    where T : CStructVar, T: CPlusPlusClass
{
    init {
        println("ManagedType INIT")
    }
    //val cleaner = createCleaner(cpp) {
    //    println("RUNNING CLEANER for $it")
    //    it.__destroy__()
    //}
}
