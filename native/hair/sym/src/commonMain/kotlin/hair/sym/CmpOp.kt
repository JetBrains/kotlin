/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.sym

enum class CmpOp {
    EQ,
    NE,

    // TODO do we really need GT when we have LT?
    U_GT,
    U_GE,
    U_LT,
    U_LE,

    S_GT,
    S_GE,
    S_LT,
    S_LE,
}