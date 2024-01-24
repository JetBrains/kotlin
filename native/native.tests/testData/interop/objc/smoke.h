#import <objc/NSObject.h>
#import <CoreFoundation/CoreFoundation.h>

@class Foo;

@protocol Printer;
@protocol Printer;

@protocol Empty
@end

@protocol Forward;
@class Forward;

void useForward1(Forward * p) {}
void useForward2(id<Forward> p) {}

typedef NSString NSStringTypedef;

@interface Foo : NSObject <Empty>
@property NSStringTypedef* name;
-(void)helloWithPrinter:(id <Printer>)printer;
@end

@interface Foo (FooExtensions)
-(void)hello;
@end

@protocol Printer
@required
-(void)print:(const char*)string;
@end

@protocol MutablePair
@required
@property (readonly) int first;
@property (readonly) int second;

-(void)update:(int)index add:(int)delta;
-(void)update:(int)index sub:(int)delta;

@end

void replacePairElements(id <MutablePair> pair, int first, int second);

int invoke1(int arg, int (^block)(int)) {
    return block(arg);
}

void invoke2(void (^block)(void)) {
    block();
}

int (^getSupplier(int x))(void);
Class (^ _Nonnull getClassGetter(NSObject* obj))(void);

extern NSString* globalString;
extern NSObject* globalObject;

int formatStringLength(NSString* format, ...);

#define STRING_MACRO @"String macro"
#define CFSTRING_MACRO CFSTR("CFString macro")

typedef NS_ENUM(int32_t, ForwardDeclaredEnum);
typedef NS_ENUM(int32_t, ForwardDeclaredEnum) {
    ZERO, ONE, TWO,
};

@protocol ObjectFactory
@required
-(id)create;
@end

id createObjectWithFactory(id<ObjectFactory> factory) {
  return [factory create];
}

@protocol CustomRetainMethods
@required
-(id)returnRetained:(id)obj __attribute__((ns_returns_retained));
-(void)consume:(id) __attribute__((ns_consumed)) obj;
-(void)consumeSelf __attribute__((ns_consumes_self));
-(void (^)(void))returnRetainedBlock:(void (^)(void))block __attribute__((ns_returns_retained));
@end

extern BOOL unexpectedDeallocation;

@interface MustNotBeDeallocated : NSObject
@end

@interface CustomRetainMethodsImpl : MustNotBeDeallocated <CustomRetainMethods>
@end

static MustNotBeDeallocated* retainedObj;
static void (^retainedBlock)(void);

void useCustomRetainMethods(id<CustomRetainMethods> p) {
  MustNotBeDeallocated* obj = [MustNotBeDeallocated new];
  retainedObj = obj; // Retain to detect possible over-release.
  [p returnRetained:obj];

  [p consume:p];
  [p consumeSelf];

  MustNotBeDeallocated* capturedObj = [MustNotBeDeallocated new];
  retainedBlock = ^{ [capturedObj description]; }; // Retain to detect possible over-release.
  [p returnRetainedBlock:retainedBlock]();
}

id getPrinterProtocolRaw() {
  return @protocol(Printer);
}

Protocol* getPrinterProtocol() {
  return @protocol(Printer);
}
