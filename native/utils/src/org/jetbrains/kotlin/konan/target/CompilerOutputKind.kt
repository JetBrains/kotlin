/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

enum class CompilerOutputKind {
    PROGRAM {
        override fun suffix(target: KonanTarget?) = ".${target!!.family.exeSuffix}"
    },
    DYNAMIC {
        override fun suffix(target: KonanTarget?) = ".${target!!.family.dynamicSuffix}"
        override fun prefix(target: KonanTarget?) = target!!.family.dynamicPrefix
    },
    STATIC {
        override fun suffix(target: KonanTarget?) = ".${target!!.family.staticSuffix}"
        override fun prefix(target: KonanTarget?) = target!!.family.staticPrefix
    },
    FRAMEWORK {
        override fun suffix(target: KonanTarget?): String = ".framework"
    },
    LIBRARY {
        override fun suffix(target: KonanTarget?) = ".klib"
    },
    BITCODE {
        override fun suffix(target: KonanTarget?) = ".bc"
    },
    TEST_BUNDLE {
        override fun suffix(target: KonanTarget?): String = ".xctest"
    },

    DYNAMIC_CACHE {
        override fun suffix(target: KonanTarget?) = ".${target!!.family.dynamicSuffix}"
        override fun prefix(target: KonanTarget?) = target!!.family.dynamicPrefix
    },
    STATIC_CACHE {
        override fun suffix(target: KonanTarget?) = ".${target!!.family.staticSuffix}"
        override fun prefix(target: KonanTarget?) = target!!.family.staticPrefix
    },
    PRELIMINARY_CACHE {
        override fun suffix(target: KonanTarget?) = ""
    };

    abstract fun suffix(target: KonanTarget? = null): String
    open fun prefix(target: KonanTarget? = null): String = ""
}
