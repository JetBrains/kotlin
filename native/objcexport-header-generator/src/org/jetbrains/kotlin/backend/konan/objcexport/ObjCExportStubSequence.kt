/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi

@InternalKotlinNativeApi
fun ObjCExportStub.closureSequence(): Sequence<ObjCExportStub> = sequence {
    val stub = this@closureSequence
    yield(stub)
    when (stub) {
        is ObjCClass -> stub.members.forEach { member ->
            yieldAll(member.closureSequence())
        }
        is ObjCMethod -> stub.parameters.forEach { parameter ->
            yield(parameter)
        }
        else -> Unit
    }
}

fun Iterable<ObjCExportStub>.closureSequence() = sequence<ObjCExportStub> {
    forEach { stub -> yieldAll(stub.closureSequence()) }
}