/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.ir.generator.toolbox

enum class FormKind {
    SIMPLE,
    PARAMETRIZED,
    PARAMETRIZED_SINGLETON,
}

val Node.formKind: FormKind
    get() = when {
        allFormParams().isEmpty() -> FormKind.SIMPLE
        allNodeParams().isEmpty() && variadicParam == null -> FormKind.PARAMETRIZED_SINGLETON
        else -> FormKind.PARAMETRIZED
    }
