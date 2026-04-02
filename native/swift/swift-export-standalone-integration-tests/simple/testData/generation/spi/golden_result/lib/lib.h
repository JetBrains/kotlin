#include <Foundation/Foundation.h>
#include <stdint.h>

NS_ASSUME_NONNULL_BEGIN

_Bool ExperimentalLibClass_bar(void * self);

NSString * ExperimentalLibClass_foo_get(void * self);

_Bool ExperimentalLibClass_foo_set__TypesOfArguments__Swift_String__(void * self, NSString * newValue);

_Bool InternalLibInterface_bar(void * self);

NSString * InternalLibInterface_foo_get(void * self);

_Bool InternalLibInterface_foo_set__TypesOfArguments__Swift_String__(void * self, NSString * newValue);

void * __root___ExperimentalLibClass_init_allocate();

_Bool __root___ExperimentalLibClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(void * __kt);

void * __root___RegularLibClass_init_allocate();

_Bool __root___RegularLibClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(void * __kt, NSString * a);

_Bool __root___RegularLibClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(void * __kt);

_Bool __root___experimentalLibFunction();

NSString * __root___experimentalLibSetter_get();

_Bool __root___experimentalLibSetter_set__TypesOfArguments__Swift_String__(NSString * newValue);

NSString * __root___experimentalProperty_get();

_Bool __root___experimentalProperty_set__TypesOfArguments__Swift_String__(NSString * newValue);

_Bool __root___fooA__TypesOfArgumentsC1__lib_ExperimentalLibClass_anyU20lib_InternalLibInterface__(void * b, void * a);

void * __root___fooA_get();

void * __root___fooB__TypesOfArgumentsE__anyU20lib_InternalLibInterface__(void * receiver);

void * __root___fooB_get();

void * __root___fooC();

void * __root___fooD();

NSString * __root___genericFunction__TypesOfArguments__anyU20lib_InternalLibInterface__(void * a);

NSString * __root___genericProperty_get__TypesOfArgumentsE__anyU20lib_InternalLibInterface__(void * receiver);

_Bool __root___internalLibFunction();

NSString * __root___internalLibSetter_get();

_Bool __root___internalLibSetter_set__TypesOfArguments__Swift_String__(NSString * newValue);

NSString * __root___internalProperty_get();

_Bool __root___internalProperty_set__TypesOfArguments__Swift_String__(NSString * newValue);

_Bool __root___normalLibFunction();

NSString * __root___returnAlias();

NS_ASSUME_NONNULL_END
