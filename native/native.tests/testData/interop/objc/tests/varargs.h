#import <Foundation/NSObject.h>

@interface TestVarargs : NSObject
-(instancetype _Nonnull)initWithFormat:(NSString*)format, ...;
+(instancetype _Nonnull)testVarargsWithFormat:(NSString*)format, ...;
@property NSString* formatted;

+(NSString* _Nonnull)stringWithFormat:(NSString*)format, ...;
+(NSObject* _Nonnull)stringWithFormat:(NSString*)format args:(void*)args;
@end

@interface TestVarargs (TestVarargsExtension)
-(instancetype _Nonnull)initWithFormat:(NSString*)format, ...;
@end

@interface TestVarargsSubclass : TestVarargs
// Test clashes:
-(instancetype _Nonnull)initWithFormat:(NSString*)format args:(void*)args;
+(NSString* _Nonnull)stringWithFormat:(NSString*)format args:(void*)args;
@end
