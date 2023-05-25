#import <Foundation/NSObject.h>

@interface InterfaceBase : NSObject
@property InterfaceBase* delegate;
@end

// Presence of property `delegate` in both InterfaceBase & IntegerProperty makes an intersection override, and compiler invokes
// K1: ObjCOverridabilityCondition.isOverridable(IntegerPropertyProtocol.delegate, InterfaceBase.delegate) and
//     ObjCOverridabilityCondition.isOverridable(InterfaceBase.delegate, IntegerPropertyProtocol.delegate)
// K2: FirCallableDeclaration.isPlatformOverriddenProperty(InterfaceBase.delegate, IntegerPropertyProtocol.delegate) and
//     ObjCOverridabilityCondition.isOverridable(IntegerPropertyProtocol.delegate, InterfaceBase.delegate) and
//     ObjCOverridabilityCondition.isOverridable(InterfaceBase.delegate, IntegerPropertyProtocol.delegate)
// Without this protocol, these invocations don't happen.

@protocol IntegerProperty
@property NSInteger delegate;
@end

@interface InterfaceDerived : InterfaceBase<IntegerProperty>
@property NSString* delegate;
@end
