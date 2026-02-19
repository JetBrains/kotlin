@file:Suppress("unused")

import kotlin.random.Random

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

val rootPkgProperty get() = Random.nextInt()

fun rootPkgFunction() = Random.nextInt()

fun rootPkgFunction(seed: Int) = Random(seed).nextInt()
