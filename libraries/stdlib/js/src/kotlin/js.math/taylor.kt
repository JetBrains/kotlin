/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js.math

const val defineTaylorNBound = """
    var epsilon = 2.220446049250313E-16;
    var taylor_2_bound = Math.sqrt(epsilon);
    var taylor_n_bound = Math.sqrt(taylor_2_bound);
"""

const val defineUpperTaylor2Bound = """
    $defineTaylorNBound
    var upper_taylor_2_bound = 1/taylor_2_bound;
"""

const val defineUpperTaylorNBound = """
    $defineUpperTaylor2Bound
    var upper_taylor_n_bound = 1/taylor_n_bound;
"""
