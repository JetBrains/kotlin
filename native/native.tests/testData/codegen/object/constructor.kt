/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// This test checks object layout can't be done in KonanLocalTest paradigm
// So should be done within former UnitKonanTest. Without it, test is useless

class A()

class B(val a:Int)

class C(val a:Int, b:Int)

fun box() = "OK"
