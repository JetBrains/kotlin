#import <XCTest/XCTest.h>
#import <xctest_runner/xctest_runner.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSString.h>

@interface XCTestLauncher : XCTestCase
@end

@implementation XCTestLauncher

+ (id)defaultTestSuite {
    return [Xctest_runnerTestCaseRunnerKt defaultTestSuiteRunner];
}

@end


@interface PrincipalExport : NSObject
- (void)run;
- (void)main;
@end

@implementation PrincipalExport
- (id)init {
    self = [super init];
    NSLog(@"=== Principal init");
    NSLog(@"%@", NSThread.callStackSymbols);
    return self;
}

- (void)run {
    NSLog(@"=== Principal runner");
}

- (void)main {
    NSLog(@"=== Principal main");
}
@end