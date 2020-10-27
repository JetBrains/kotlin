#import <objc/NSObject.h>

static NSObject* __weak globalObject = nil;

void setObject(NSObject* obj) {
  globalObject = obj;
}

// Make sure this function persists, because the test expects to find this function in the stack trace.
__attribute__((noinline))
bool isObjectAliveShouldCrash() {
  return globalObject != nil;
}
