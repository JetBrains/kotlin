#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

__attribute__((objc_runtime_name("CC")))
__attribute__((swift_name("CC")))
@interface CallingConventions : NSObject

+ (uint64_t)regular:(uint64_t)arg;
- (uint64_t)regular:(uint64_t)arg;

+ (uint64_t)direct:(uint64_t)arg __attribute__((objc_direct));
- (uint64_t)direct:(uint64_t)arg __attribute__((objc_direct));

@end

@interface CallingConventions(Ext)

+ (uint64_t)regularExt:(uint64_t)arg;
- (uint64_t)regularExt:(uint64_t)arg;

+ (uint64_t)directExt:(uint64_t)arg __attribute__((objc_direct));
- (uint64_t)directExt:(uint64_t)arg __attribute__((objc_direct));

@end

__attribute__((objc_runtime_name("CCH")))
__attribute__((swift_name("CCH")))
@interface CallingConventionsHeir : CallingConventions
@end

NS_ASSUME_NONNULL_END