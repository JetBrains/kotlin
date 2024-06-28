// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false

// FREE_CINTEROP_ARGS: -compiler-option -F$generatedSourcesDir/cinterop
// MODULE: cinterop
// FILE: objclib.def
language = Objective-C
modules = Foo
---
static int getDefInt() {
    return 2;
}

static int getFrameworkIntFromDef() {
    return getFrameworkInt();
}

// FILE: Foo.framework/Headers/Foo.h
static int getFrameworkInt() {
    return 1;
}

// FILE: Foo.framework/Modules/module.modulemap
framework module Foo {
    umbrella header "Foo.h"
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import kotlin.test.*
import objclib.*

fun box(): String {
    assertEquals(1, getFrameworkInt())
    assertEquals(2, getDefInt())
    assertEquals(1, getFrameworkIntFromDef())
    return "OK"
}
