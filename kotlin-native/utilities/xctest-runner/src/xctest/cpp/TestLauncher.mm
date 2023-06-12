#import <XCTest/XCTest.h>

#import "Common.h"
#import "Runtime.h"
#import "ObjCExport.h"

extern "C" OBJ_GETTER0(Konan_create_testSuite);

@interface XCTestLauncher : XCTestCase
@end

@implementation XCTestLauncher
/**
 * Test suites factory.
 *
 * This is a starting point for XCTest to get the test suites with test cases.
 * K/N dynamically adds test suites for Kotlin tests.
 * @see Konan_create_testSuite
 */
+ (id)defaultTestSuite {
    Kotlin_initRuntimeIfNeeded();
    Kotlin_mm_switchThreadStateRunnable();
    KRef result = nil;
    Konan_create_testSuite(&result);
    id retainedResult = Kotlin_ObjCExport_refToRetainedObjC(result);
    Kotlin_mm_switchThreadStateNative();
    return retainedResult;
}

// TODO: do we need to shutdown? when: dealloc or setUp and tearDown?
- (void)dealloc {
    Kotlin_shutdownRuntime();
}
@end
