#include "objclib.h"

@implementation WithFinalizer {
    Finalizer finalizer_;
    void* arg_;
}

- (void)dealloc {
    if (finalizer_) {
        finalizer_(arg_);
    }
}

- (void)setFinalizer:(Finalizer)finalizer arg:(void*)arg {
    finalizer_ = finalizer;
    arg_ = arg;
}

@end
