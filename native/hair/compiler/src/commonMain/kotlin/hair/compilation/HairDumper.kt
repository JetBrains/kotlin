/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.compilation

import hair.sym.HairFunction
import hair.transform.withGCM
import hair.utils.generateGraphviz

abstract class HairDumper {
    fun dump(compilation: FunctionCompilation, title: String) {
        with (compilation.session) {
            val (dump, withGCM) = try {
                withGCM {
                    generateGraphviz() to true
                }
            } catch (_: Throwable) {
                generateGraphviz() to false
            }
            dumpImpl(
                f = compilation.function,
                title = title + if (withGCM) "" else "_NO_GCM",
                contents = dump
            )
        }
    }

    abstract fun dumpImpl(f: HairFunction, title: String, contents: String)
}