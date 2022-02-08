#import "kt42482.h"
#import "assert.h"
#import <objc/runtime.h>

BOOL kt42482Deallocated = NO;
id kt42482Global = nil;

@implementation KT42482
-(int)fortyTwo {
    return 41;
}

-(void)dealloc {
    kt42482Deallocated = YES;
}
@end;

int fortyTwoSwizzledImp(id self, SEL _cmd) {
    return 43;
}

void kt42482Swizzle(id obj) {
    Class oldClass = object_getClass(obj);

    SEL selector = @selector(fortyTwo);

    Class newClass = objc_allocateClassPair(oldClass, "KT42482Swizzled", 0);
    assert(newClass != nil);
    objc_registerClassPair(newClass);

    Method method = class_getInstanceMethod([KT42482 class], selector);
    assert(method != nil);
    class_addMethod(newClass, selector, (IMP)&fortyTwoSwizzledImp, method_getTypeEncoding(method));

    object_setClass(obj, newClass);
}
