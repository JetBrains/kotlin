/*
 * Copyright 2016-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package cases.whenMappings

enum class SampleEnum {
    A,
    B,
    C
}

fun SampleEnum.deacronimize() = when (this) {
    SampleEnum.A -> "Apple"
    SampleEnum.B -> "Biscuit"
    SampleEnum.C -> "Cinnamon"
}


inline fun SampleEnum.switch(thenA: () -> Unit, thenB: () -> Unit, thenC: () -> Unit) = when (this) {
    SampleEnum.C -> thenC()
    SampleEnum.B -> thenB()
    SampleEnum.A -> thenA()
}
