/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.sym

object RuntimeInterface {
    class RTFun(override val name: String, override val resultHairType: HairType) : HairFunction {
        override fun toString(): String = "RTFun($name)"
    }

    val isSubtype = RTFun("isSubtype", HairType.INT)
}