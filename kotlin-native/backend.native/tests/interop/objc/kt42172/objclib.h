#import <objc/NSObject.h>

typedef void (*Finalizer)(void*);

@interface WithFinalizer : NSObject

- (void)setFinalizer:(Finalizer)finalizer arg:(void*)arg;

@end
