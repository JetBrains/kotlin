#import <XCTest/XCTest.h>
#import "Common.h"

// TODO: maybe just __attribute__((used)) instead of RUNTIME_USED
extern "C" RUNTIME_USED id Konan_create_testSuite();

@interface XCTestLauncher : XCTestCase
@end

@implementation XCTestLauncher
// This is a starting point for XCTest to get the test suite with test cases
+ (id)defaultTestSuite {
    return Konan_create_testSuite();
}
@end
