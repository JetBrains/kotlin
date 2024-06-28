/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
// TARGET_BACKEND: NATIVE
// MODULE: lib1
// FILE: lib1.def
language=Objective-C
headers=lib1.h

// FILE: lib1.h
@class Foo;
@protocol Bar;
struct Baz;

// MODULE: lib2(lib1)
// FILE: lib2.def
language=Objective-C
headers=lib2.h

// FILE: lib2.h
#import "../lib1/lib1.h"

Foo* createFoo() {
    return 0;
}

id<Bar> createBar() {
    return 0;
}

struct Baz* createBaz() {
    return 0;
}

// MODULE: main(lib1,lib2)
// FILE: main.kt
import kotlinx.cinterop.CPointer
import lib1.*
import lib2.*
import cnames.structs.Baz
import objcnames.classes.Foo
import objcnames.protocols.BarProtocol

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun box(): String {
    val foo: Foo? = createFoo()
    if (foo !== null) return "FAIL 1"

    val bar: BarProtocol? = createBar()
    if (bar !== null) return "FAIL 2"

    val baz: CPointer<Baz>? = createBaz()
    if (baz !== null) return "FAIL 3"

    return "OK"
}
