/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.cli.common.isWindows
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.safeName
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator.Companion.getJsArtifactSimpleName
import org.jetbrains.kotlin.utils.addToStdlib.runIf

private typealias K = IrModuleFragment
private typealias V = String

class JsIrModuleToPath(val testServices: TestServices, shouldProvidePaths: Boolean) : Map<K, V> {
    override val size = if (!shouldProvidePaths) 0 else 1
    override val entries = emptySet<Map.Entry<K, V>>()
    override val keys = emptySet<K>()
    override val values = emptyList<V>()

    override fun isEmpty() = size == 0
    override fun containsKey(key: K): Boolean = !isEmpty()
    override fun containsValue(value: V): Boolean = !isEmpty()

    override operator fun get(key: K): V? {
        return runIf(!isEmpty()) {
            "./${getJsArtifactSimpleName(testServices, key.safeName)}_v5.mjs".run {
                if (isWindows) minify() else this
            }
        }
    }
}