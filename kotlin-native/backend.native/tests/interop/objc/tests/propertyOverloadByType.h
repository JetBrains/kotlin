#import <Foundation/NSObject.h>

@interface InterfaceBase : NSObject
@property (readwrite) InterfaceBase* delegate;
@end

@protocol IntegerProperty
@property (readonly) NSInteger delegate;
@end

@protocol UIntegerProperty
@property (readonly) NSUInteger delegate;
@end

@interface InterfaceDerived : InterfaceBase<IntegerProperty, UIntegerProperty>
// property `delegate` is affected by parent declarations:
// - `InterfaceBase* InterfaceBase.delegate` and
// - `NSInteger IntegerProperty.delegate`
@property (readonly) InterfaceBase* delegate;
@end
