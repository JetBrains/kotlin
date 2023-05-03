#include "objclib.h"

void performSelector0(id target, NSString* selectorName) {
    NSLog(@"performSelector0(%@, %@)", target, selectorName);
    // Ignoring return value, because ObjCAction does not allow one.
    [target performSelector:NSSelectorFromString(selectorName)];
}

void performSelectorInNewThread0(id target, NSString* selectorName) {
    NSLog(@"performSelectorInNewThread0(%@, %@)", target, selectorName);
    NSThread* thread = [[NSThread alloc] initWithBlock:^{
        performSelector0(target, selectorName);
    }];
    [thread start];
}

void performSelector1(id target, NSString* selectorName, id arg1) {
    NSLog(@"performSelector1(%@, %@, %@)", target, selectorName, arg1);
    // Ignoring return value, because ObjCAction does not allow one.
    [target performSelector:NSSelectorFromString(selectorName) withObject:arg1];
}

void performSelectorInNewThread1(id target, NSString* selectorName, id arg1) {
    NSLog(@"performSelectorInNewThread1(%@, %@, %@)", target, selectorName, arg1);
    // Use NSThread method directly just for variety.
    NSThread* thread = [[NSThread alloc] initWithTarget:target selector:NSSelectorFromString(selectorName) object:arg1];
    [thread start];
}

void performSelector2(id target, NSString* selectorName, id arg1, id arg2) {
    NSLog(@"performSelector2(%@, %@, %@, %@)", target, selectorName, arg1, arg2);
    // Ignoring return value, because ObjCAction does not allow one.
    [target performSelector:NSSelectorFromString(selectorName) withObject:arg1 withObject:arg2];
}

void performSelectorInNewThread2(id target, NSString* selectorName, id arg1, id arg2) {
    NSLog(@"performSelectorInNewThread2(%@, %@, %@, %@)", target, selectorName, arg1, arg2);
    NSThread* thread = [[NSThread alloc] initWithBlock:^{
        performSelector2(target, selectorName, arg1, arg2);
    }];
    [thread start];
}

void setProperty(id target, NSString* propertyName, id value) {
    NSLog(@"setProperty(%@, %@, %@)", target, propertyName, value);
    [target setValue:value forKey:propertyName];
}