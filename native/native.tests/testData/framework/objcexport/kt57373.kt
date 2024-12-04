/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

interface IKt57373 {
    val bar: Int
}

class DKt57373(foo: IKt57373) : IKt57373 by foo

class CKt57373 : IKt57373 {
    override val bar: Int = 42
}
