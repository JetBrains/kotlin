// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false
// LANGUAGE: +CompanionBlocks
// MODULE: cinterop
// FILE: cinterop.def
language = Objective-C
headers = type.h
// FILE: type.h
#import <Foundation/Foundation.h>

void setInitLogger(void (^logger)(NSString*));

@interface MyClassForCtor: NSObject
@end

@protocol MyInterface
- (NSString*)foo;
@end
// FILE: type.m
#import "type.h"

void (^initLogger)(NSString*) = nil;

void setInitLogger(void (^logger)(NSString*)) {
    initLogger = logger;
}

@implementation MyClassForCtor
+ (void)initialize {
    if (self == [MyClassForCtor class]) {
        initLogger(@"MyClass.initialize");
    }
}
@end
// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import cinterop.*

var l = ""
fun log(t: String) {
    l += t + "\n"
}

class C: MyClassForCtor(), MyInterfaceProtocol {
    override fun foo() = "OK"
    companion {
        val x = run<_> {
            log("C.companion")
            "OK"
        }
    }
    init {
        log("C.init")
    }
}

fun box(): String {
    setInitLogger {
        log(it!!)
    }
    val c = C()
    if (l != "MyClass.initialize\nC.companion\nC.init\n") {
        return "FAIL: $l"
    }
    return c.foo()
}

