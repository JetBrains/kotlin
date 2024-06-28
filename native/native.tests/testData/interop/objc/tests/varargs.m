#import <stdlib.h>
#import <Foundation/NSString.h>
#import "varargs.h"

@implementation TestVarargs
-(instancetype _Nonnull)initWithFormat:(NSString*)format, ... {
    self = [super init];

    va_list args;
    va_start(args, format);
    self.formatted = [[NSString alloc] initWithFormat:format arguments:args];
    va_end(args);

    return self;
}

+(instancetype _Nonnull)testVarargsWithFormat:(NSString*)format, ... {
    TestVarargs* result = [[self alloc] init];

    va_list args;
    va_start(args, format);
    result.formatted = [[NSString alloc] initWithFormat:format arguments:args];
    va_end(args);

    return result;
}

+(NSString* _Nonnull)stringWithFormat:(NSString*)format, ... {
    va_list args;
    va_start(args, format);
    NSString* result = [[NSString alloc] initWithFormat:format arguments:args];
    va_end(args);

    return result;
}

+(NSObject* _Nonnull)stringWithFormat:(NSString*)format args:(void*)args {
    abort();
}

@end

@implementation TestVarargsSubclass
-(instancetype _Nonnull)initWithFormat:(NSString*)format args:(void*)args {
    abort();
}

+(NSString* _Nonnull)stringWithFormat:(NSString*)format args:(void*)args {
    abort();
}
@end