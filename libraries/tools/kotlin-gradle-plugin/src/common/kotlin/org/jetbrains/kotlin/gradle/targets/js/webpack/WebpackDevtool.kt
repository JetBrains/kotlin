/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.webpack

object WebpackDevtool {
    const val EVAL = "eval"
    const val EVAL_CHEAP_SOURCE_MAP = "eval-cheap-source-map"
    const val EVAL_CHEAP_MODULE_SOURCE_MAP = "eval-cheap-module-source-map"
    const val EVAL_SOURCE_MAP = "eval-source-map"
    const val EVAL_NOSOURCES_SOURCE_MAP = "eval-nosources-source-map"
    const val EVAL_NOSOURCES_CHEAP_SOURCE_MAP = "eval-nosources-cheap-source-map"
    const val EVAL_NOSOURCES_CHEAP_MODULE_SOURCE_MAP = "eval-nosources-cheap-module-source-map"
    const val CHEAP_SOURCE_MAP = "cheap-source-map"
    const val CHEAP_MODULE_SOURCE_MAP = "cheap-module-source-map"
    const val INLINE_CHEAP_SOURCE_MAP = "inline-cheap-source-map"
    const val INLINE_CHEAP_MODULE_SOURCE_MAP = "inline-cheap-module-source-map"
    const val INLINE_SOURCE_MAP = "inline-source-map"
    const val INLINE_NOSOURCES_SOURCE_MAP = "inline-nosources-source-map"
    const val INLINE_NOSOURCES_CHEAP_SOURCE_MAP = "inline-nosources-cheap-source-map"
    const val INLINE_NOSOURCES_CHEAP_MODULE_SOURCE_MAP = "inline-nosources-cheap-module-source-map"
    const val SOURCE_MAP = "source-map"
    const val HIDDEN_SOURCE_MAP = "hidden-source-map"
    const val HIDDEN_NOSOURCES_SOURCE_MAP = "hidden-nosources-source-map"
    const val HIDDEN_NOSOURCES_CHEAP_SOURCE_MAP = "hidden-nosources-cheap-source-map"
    const val HIDDEN_NOSOURCES_CHEAP_MODULE_SOURCE_MAP = "hidden-nosources-cheap-module-source-map"
    const val HIDDEN_CHEAP_SOURCE_MAP = "hidden-cheap-source-map"
    const val HIDDEN_CHEAP_MODULE_SOURCE_MAP = "hidden-cheap-module-source-map"
    const val NOSOURCES_SOURCE_MAP = "nosources-source-map"
    const val NOSOURCES_CHEAP_SOURCE_MAP = "nosources-cheap-source-map"
    const val NOSOURCES_CHEAP_MODULE_SOURCE_MAP = "nosources-cheap-module-source-map"
}