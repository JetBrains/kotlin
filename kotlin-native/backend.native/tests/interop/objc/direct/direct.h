#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

__attribute__((objc_runtime_name("CC")))
__attribute__((swift_name("CC")))
@interface CallingConventions : NSObject

+ (NSUInteger)regular:(NSUInteger)arg;
- (NSUInteger)regular:(NSUInteger)arg;

+ (NSUInteger)direct:(NSUInteger)arg __attribute__((objc_direct));
- (NSUInteger)direct:(NSUInteger)arg __attribute__((objc_direct));

@end

@interface CallingConventions(Ext)

+ (NSUInteger)regularExt:(NSUInteger)arg;
- (NSUInteger)regularExt:(NSUInteger)arg;

+ (NSUInteger)directExt:(NSUInteger)arg __attribute__((objc_direct));
- (NSUInteger)directExt:(NSUInteger)arg __attribute__((objc_direct));

@end

__attribute__((objc_runtime_name("CCH")))
__attribute__((swift_name("CCH")))
@interface CallingConventionsHeir : CallingConventions
@end

NS_ASSUME_NONNULL_END