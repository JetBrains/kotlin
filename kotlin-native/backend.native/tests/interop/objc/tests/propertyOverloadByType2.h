#import <Foundation/NSObject.h>

@interface InterfaceBase2 : NSObject
@property (readwrite) InterfaceBase2* delegate;
@end

@protocol IntegerProperty2
@property (readonly) NSInteger delegate;
@end

@interface InterfaceDerived2 : InterfaceBase2<IntegerProperty2>
// property `delegate` would be made as an intersection override from
// - `InterfaceBase* InterfaceBase.delegate` and
// - `NSInteger IntegerProperty.delegate`
@end
