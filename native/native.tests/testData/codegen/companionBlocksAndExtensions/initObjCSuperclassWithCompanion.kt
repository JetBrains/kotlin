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

@interface MyClassForCompanion: NSObject
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

@implementation MyClassForCompanion
+ (void)initialize {
    if (self == [MyClassForCompanion class]) {
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

class C: MyClassForCompanion(), MyInterfaceProtocol {
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
    val result = C.x
    // TODO(KT-90390): consider running `+initialize` on this ObjC class;
    //                 it should then be "MyClass.initialize\nC.companion\n"
    if (l != "C.companion\n") {
        return "FAIL: $l"
    }
    return result
}

