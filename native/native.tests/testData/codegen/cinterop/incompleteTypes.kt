// FREE_CINTEROP_ARGS: -header incompleteTypes.h

// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: incompleteTypes.def
# The def file is intentionally empty
# `incompleteTypes.h` is meant to be included via `-header` option of cinterop tool

// FILE: incompleteTypes.h
#ifdef __cplusplus
extern "C" {
#endif

// Forward declaration.
struct S;
extern struct S s;

const char* getContent(struct S* s);
void setContent(struct S* s, const char* name);

union U;
extern union U u;

double getDouble(union U* u);
void setDouble(union U* u, double value);

// Global array of unknown size.
extern char array[];


int arrayLength();
void setArrayValue(char* array, char value);

#ifdef __cplusplus
}
#endif

// FILE: incompleteTypes.cpp
#include <string>
#include "incompleteTypes.h"

extern "C" {

struct S {
    std::string name;
};

S s = {
    .name = "initial"
};

void setContent(struct S* s, const char* name) {
    // Note that copy here is intentional: we use it as a workaround
    // for short lifetime of copy of the passed Kotlin string.
        s->name = name;
}

const char* getContent(struct S* s) {
    return s->name.c_str();
}

union U {
    float f;
    double d;
};

void setDouble(union U* u, double value) {
        u->d = value;
}

double getDouble(union U* u) {
    return u->d;
}

union U u = {
    .d = 0.0
};

char array[5] = { 0, 0, 0, 0, 0 };

void setArrayValue(char* array, char value) {
    for (int i = 0; i < 5; ++i) {
    array[i] = value;
}
}

int arrayLength() {
    return sizeof(array) / sizeof(char);
}

}

// MODULE: main(cinterop)
// FILE: main.kt

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import incompleteTypes.*
import kotlinx.cinterop.*
import kotlin.test.*

fun box(): String {
    assertNotNull(s.ptr)
    assertNotNull(u.ptr)
    assertNotNull(array)

    assertEquals("initial", getContent(s.ptr)?.toKString())
    setContent(s.ptr, "yo")
    val ptr = getContent(s.ptr)
    assertEquals("yo", ptr?.toKString())

    assertEquals(0.0, getDouble(u.ptr))
    setDouble(u.ptr, Double.MIN_VALUE)
    assertEquals(Double.MIN_VALUE, getDouble(u.ptr))

    for (i in 0 until arrayLength()) {
        assertEquals(0x0, array[i])
    }
    setArrayValue(array, 0x1)
    for (i in 0 until arrayLength()) {
        assertEquals(0x1, array[i])
    }
    return "OK"
}
