// TARGET_BACKEND: NATIVE
// `import objcnames` somehow works only with NATIVE_STANDALONE test directive
// NATIVE_STANDALONE
// DISABLE_NATIVE: isAppleTarget=false

// MODULE: a
// FILE: a.def
language = Objective-C
---
#import <Foundation/Foundation.h>
#include <stdio.h>

struct ForwardDeclaredStruct;
@class ForwardDeclaredClass;
@protocol ForwardDeclaredProtocol;

NSString* consumeProtocol(id<ForwardDeclaredProtocol> s) {
	return [NSString stringWithUTF8String:"Protocol"];
}
NSString* consumeClass(ForwardDeclaredClass* s) {
	return [NSString stringWithUTF8String:"Class"];
}
NSString* consumeStruct(struct ForwardDeclaredStruct* s) {
	return [NSString stringWithUTF8String:"Struct"];
}

// MODULE: b
// FILE: b.def
language = Objective-C
headers = b.h
---

// FILE: b.h
#define NS_FORMAT_ARGUMENT(X)
#import <Foundation/Foundation.h>

@protocol ForwardDeclaredProtocol
@end

@interface ForwardDeclaredProtocolImpl : NSObject<ForwardDeclaredProtocol>
@end;


@interface ForwardDeclaredClass : NSObject
@end;

struct ForwardDeclaredStruct {};

id<ForwardDeclaredProtocol> produceProtocol();
ForwardDeclaredClass* produceClass();
struct ForwardDeclaredStruct* produceStruct();

// FILE: b.m
#import "b.h"

@implementation ForwardDeclaredProtocolImpl : NSObject
@end;

@implementation ForwardDeclaredClass : NSObject
@end;

id<ForwardDeclaredProtocol> produceProtocol() {
	return [ForwardDeclaredProtocolImpl new];
}

ForwardDeclaredClass* produceClass() {
	return [ForwardDeclaredClass new];
}

struct ForwardDeclaredStruct S;

struct ForwardDeclaredStruct* produceStruct() {
	return &S;
}

// MODULE: lib(a)
// FILE: lib.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import cnames.structs.ForwardDeclaredStruct
import objcnames.classes.ForwardDeclaredClass
import objcnames.protocols.ForwardDeclaredProtocolProtocol
import a.*
import kotlinx.cinterop.*

fun testStruct(s: Any?) = consumeStruct(s as CPointer<ForwardDeclaredStruct>)
fun testClass(s: Any?) = consumeClass(s as ForwardDeclaredClass)
fun testProtocol(s: Any?) = consumeProtocol(s as ForwardDeclaredProtocolProtocol)


fun <T : ForwardDeclaredClass> testClass2Impl(s: Any?) = consumeClass(s as T)
fun <T : ForwardDeclaredProtocolProtocol> testProtocol2Impl(s: Any?) = consumeProtocol(s as T)

fun testClass2(s: Any?) = testClass2Impl<ForwardDeclaredClass>(s)
fun testProtocol2(s: Any?) = testProtocol2Impl<ForwardDeclaredProtocolProtocol>(s)

// MODULE: main(lib, b)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import a.*
import b.*


fun box(): String {
	if (testStruct(produceStruct()) != "Struct") throw IllegalStateException()
	if (testClass(produceClass()) != "Class") throw IllegalStateException()
    if (testProtocol(produceProtocol()) != "Protocol") throw IllegalStateException()
	if (testClass2(produceClass()) != "Class") throw IllegalStateException()
	if (testProtocol2(produceProtocol()) != "Protocol") throw IllegalStateException()

	return "OK"
}