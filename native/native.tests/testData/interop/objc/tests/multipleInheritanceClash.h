#import <Foundation/NSObject.h>

@interface MultipleInheritanceClashBase : NSObject
@property (nonnull) MultipleInheritanceClashBase* delegate;
@end

@protocol MultipleInheritanceClash
@optional
@property (nullable) id<MultipleInheritanceClash> delegate;
@end

@interface MultipleInheritanceClash1 : MultipleInheritanceClashBase <MultipleInheritanceClash>
@end

@interface MultipleInheritanceClash2 : MultipleInheritanceClashBase <MultipleInheritanceClash>
@property MultipleInheritanceClashBase* delegate;
@end