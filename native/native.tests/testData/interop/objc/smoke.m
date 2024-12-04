#import <stdio.h>
#import <Foundation/NSString.h>
#import "smoke.h"

@interface CPrinter : NSObject <Printer>
-(void)print:(const char*)string;
@end

@implementation CPrinter
-(void)print:(const char*)string {
    printf("%s\n", string);
    fflush(stdout);
}
@end

@implementation Foo

@synthesize name;

-(instancetype)init {
    if (self = [super init]) {
        self.name = @"World";
    }
    return self;
}

-(void)helloWithPrinter:(id <Printer>)printer {
    NSString* message = [NSString stringWithFormat:@"Hello, %@!", self.name];
    [printer print:message.UTF8String];
}

-(void)dealloc {
    printf("Deallocated\n");
}

@end

@implementation Foo (FooExtensions)

-(void)hello {
    CPrinter* printer = [[CPrinter alloc] init];
    [self helloWithPrinter:printer];
}
@end

void replacePairElements(id <MutablePair> pair, int first, int second) {
    [pair update:0 add:(first - pair.first)];
    [pair update:1 sub:(pair.second - second)];
}

int (^getSupplier(int x))(void) {
    return ^{
        return x;
    };
}

Class (^ _Nonnull getClassGetter(NSObject* obj))() {
    return ^{ return obj.class; };
}

NSString* globalString = @"Global string";
NSObject* globalObject = nil;

int formatStringLength(NSString* format, ...) {
  va_list args;
  va_start(args, format);
  NSString* result = [[NSString alloc] initWithFormat:format arguments:args];
  va_end(args);
  return result.length;
}

BOOL unexpectedDeallocation = NO;

@implementation MustNotBeDeallocated
-(void)dealloc {
  unexpectedDeallocation = YES;
}
@end

static CustomRetainMethodsImpl* retainedCustomRetainMethodsImpl;

@implementation CustomRetainMethodsImpl
-(id)returnRetained:(id)obj __attribute__((ns_returns_retained)) {
    return obj;
}

-(void)consume:(id) __attribute__((ns_consumed)) obj {
}

-(void)consumeSelf __attribute__((ns_consumes_self)) {
  retainedCustomRetainMethodsImpl = self; // Retain to detect possible over-release.
}

-(void (^)(void))returnRetainedBlock:(void (^)(void))block __attribute__((ns_returns_retained)) {
    return block;
}
@end
