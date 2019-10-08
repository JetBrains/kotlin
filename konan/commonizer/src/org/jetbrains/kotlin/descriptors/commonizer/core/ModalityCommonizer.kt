/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.Modality

interface ModalityCommonizer : Commonizer<Modality, Modality> {
    companion object {
        fun default(): ModalityCommonizer = DefaultModalityCommonizer()
    }
}

private class DefaultModalityCommonizer : ModalityCommonizer {
    private var temp: Modality? = null
    private var error = false

    override val result: Modality
        get() = temp?.takeIf { !error } ?: throw IllegalCommonizerStateException()

    override fun commonizeWith(next: Modality): Boolean {
        if (error)
            return false

        val temp = temp
        this.temp = if (temp != null) getNext(temp, next) else next
        error = this.temp == null

        return !error
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun getNext(current: Modality, next: Modality): Modality? = when {
        current == Modality.FINAL && next == Modality.OPEN -> Modality.FINAL
        current == Modality.OPEN && next == Modality.FINAL -> Modality.FINAL
        current == next -> current
        else -> null
    }
}
