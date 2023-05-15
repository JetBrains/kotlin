/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

class VArrayPerSizeIteratorStateHolder(@JvmField val array: VArrayWrapperPerSize, @JvmField var index: Int)

class VArrayTwoArraysIteratorStateHolder(@JvmField val array: VArrayWrapperTwoArrays, @JvmField var index: Int)