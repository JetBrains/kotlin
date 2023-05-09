/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
fun main(args: Array<String>) {
    kotlin.native.runtime.GC.collect()
}

