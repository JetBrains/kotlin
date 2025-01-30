#include <objc/NSObject.h>
#include <Foundation/NSArray.h>

void task(id (^produceFinalizables)(), void(^collect)(), void(^schedule)(), void(^consume)(id));

uintptr_t useArray(NSArray*);
