#include <Foundation/Foundation.h>
#include <stdint.h>

NS_ASSUME_NONNULL_BEGIN

int32_t Derived_checked(void * self);

int32_t Derived_ignorable(void * self);

int32_t Foo_ignorableMember(void * self);

int32_t Foo_member(void * self);

int32_t Foo_property_get(void * self);

void * __root___Derived_init_allocate();

_Bool __root___Derived_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(void * __kt);

void * __root___Foo_init_allocate();

_Bool __root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(void * __kt);

int32_t __root___function();

int32_t __root___ignorable();

_Bool __root___nothingFunction() __attribute((noreturn));

_Bool __root___unitFunction();

NS_ASSUME_NONNULL_END
