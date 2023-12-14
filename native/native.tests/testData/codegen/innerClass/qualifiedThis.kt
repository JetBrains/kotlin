/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

open class ABase
{
    open fun zzz() = "a_base"
}

open class BBase
{
    open fun zzz() = "b_base"
}

class D() {
    val z = "d"
}

class A: ABase() { // implicit label @A
    val z = "a"
    override fun zzz() = "a"
    inner class B: BBase() { // implicit label @B
        val z = "b"
        override fun zzz() = "b"
        fun D.foo() : String { // implicit label @foo
            if(this@A.z != "a") return "Fail1"
            if(this@B.z != "b") return "Fail2"

            if(super@A.zzz() != "a_base") return "Fail3"
            if(super<BBase>.zzz() != "b_base") return "Fail4"
            if(super@B.zzz() != "b_base") return "Fail5"
            if(this@A.zzz() != "a") return "Fail6"
            if(this@B.zzz() != "b") return "Fail7"

            if(this.z != "d") return "Fail8"

            return "OK"
        }

        fun bar(d: D): String {
            return d.foo()
        }
    }
}

fun box() = A().B().bar(D())
