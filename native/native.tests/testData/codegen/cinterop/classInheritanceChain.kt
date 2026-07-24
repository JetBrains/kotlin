// TARGET_BACKEND: NATIVE

// MODULE: cinterop
// FILE: sample.def
language = Objective-C
headers = sample.h

// FILE: sample.h
#ifndef SAMPLE_H
#define SAMPLE_H

__attribute__((objc_root_class))
@interface C0
@end

@interface C1 : C0
@end

@interface C2 : C1
@end

@interface C3 : C2
@end

@interface C4 : C3
@end

@interface C5 : C4
@end

@interface C6 : C5
@end

@interface C7 : C6
@end

@interface C8 : C7
@end

@interface C9 : C8
@end

@interface C10 : C9
@end

@interface C11 : C10
@end

@interface C12 : C11
@end

@interface C13 : C12
@end

@interface C14 : C13
@end

@interface C15 : C14
@end

@interface C16 : C15
@end

@interface C17 : C16
@end

@interface C18 : C17
@end

@interface C19 : C18
@end

@interface C20 : C19
@end

@interface C21 : C20
@end

@interface C22 : C21
@end

@interface C23 : C22
@end

@interface C24 : C23
@end

@interface C25 : C24
@end

@interface C26 : C25
@end

@interface C27 : C26
@end

@interface C28 : C27
@end

@interface C29 : C28
@end

@interface C30 : C29
@end

void use_C30(C30 *obj);

#endif

// FILE: sample.m
#include "sample.h"

void use_C30(C30 *obj)
{
    (void)obj;
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import sample.use_C30

fun box(): String {
    use_C30(null)
    return "OK"
}
