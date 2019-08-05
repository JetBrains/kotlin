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
    private enum class State {
        EMPTY {
            override fun getNext(modality: Modality) = when (modality) {
                Modality.ABSTRACT -> CAN_HAVE_ONLY_ABSTRACT
                Modality.SEALED -> CAN_HAVE_ONLY_SEALED
                Modality.FINAL -> HAS_FINAL
                Modality.OPEN -> HAS_OPEN
            }
        },
        ERROR {
            override fun getNext(modality: Modality) = ERROR
        },
        CAN_HAVE_ONLY_SEALED {
            override fun getNext(modality: Modality) = if (modality == Modality.SEALED) this else ERROR
        },
        CAN_HAVE_ONLY_ABSTRACT {
            override fun getNext(modality: Modality) = if (modality == Modality.ABSTRACT) this else ERROR
        },
        HAS_FINAL {
            override fun getNext(modality: Modality) = when (modality) {
                Modality.FINAL -> this
                Modality.OPEN -> HAS_FINAL_AND_OPEN
                else -> ERROR
            }
        },
        HAS_OPEN {
            override fun getNext(modality: Modality) = when (modality) {
                Modality.FINAL -> HAS_FINAL_AND_OPEN
                Modality.OPEN -> this
                else -> ERROR
            }
        },
        HAS_FINAL_AND_OPEN {
            override fun getNext(modality: Modality) = when (modality) {
                Modality.FINAL, Modality.OPEN -> this
                else -> ERROR
            }
        };

        abstract fun getNext(modality: Modality): State
    }

    private var state = State.EMPTY

    override val result: Modality
        get() = when (state) {
            State.CAN_HAVE_ONLY_SEALED -> Modality.SEALED
            State.CAN_HAVE_ONLY_ABSTRACT -> Modality.ABSTRACT
            State.HAS_FINAL, State.HAS_FINAL_AND_OPEN -> Modality.FINAL
            State.HAS_OPEN -> Modality.OPEN
            else -> error("Modality can't be commonized")
        }

    override fun commonizeWith(next: Modality): Boolean {
        state = state.getNext(next)
        return state != State.ERROR
    }
}
