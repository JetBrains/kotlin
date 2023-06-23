#import <Foundation/NSObject.h>

@interface InterfaceBase : NSObject
@property (readwrite) InterfaceBase* delegate;
@end

@protocol IntegerProperty
@property (readonly) NSInteger delegate;
@end

@interface InterfaceDerived : InterfaceBase<IntegerProperty>
// property `delegate` is affected by parent declarations:
// - `InterfaceBase* InterfaceBase.delegate` and
// - `NSInteger IntegerProperty.delegate`
@property (readonly) InterfaceBase* delegate;
@end
