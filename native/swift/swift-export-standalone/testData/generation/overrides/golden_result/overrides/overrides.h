#include <Foundation/Foundation.h>
#include <stdint.h>

void Child_finalOverrideFunc(uintptr_t self);

uintptr_t Child_nonoverride(uintptr_t self) __attribute((noreturn));

uintptr_t Child_objectFunc__TypesOfArguments__overrides_Child__(uintptr_t self, uintptr_t arg);

uintptr_t Child_objectOptionalFunc__TypesOfArguments__overrides_Child__(uintptr_t self, uintptr_t arg);

uintptr_t Child_objectOptionalVar_get(uintptr_t self);

uintptr_t Child_objectVar_get(uintptr_t self);

void Child_overrideChainFunc(uintptr_t self);

int32_t Child_primitiveTypeFunc__TypesOfArguments__Swift_Int32__(uintptr_t self, int32_t arg);

int32_t Child_primitiveTypeVar_get(uintptr_t self);

uintptr_t Child_subtypeObjectFunc__TypesOfArguments__overrides_Child__(uintptr_t self, uintptr_t arg);

uintptr_t Child_subtypeObjectVar_get(uintptr_t self);

uintptr_t Child_subtypeOptionalObjectFunc(uintptr_t self);

uintptr_t Child_subtypeOptionalObjectVar_get(uintptr_t self);

int32_t Child_subtypeOptionalPrimitiveFunc(uintptr_t self);

int32_t Child_subtypeOptionalPrimitiveVar_get(uintptr_t self);

void GrandChild_finalOverrideHopFunc(uintptr_t self);

void GrandChild_hopFunc(uintptr_t self);

void GrandChild_overrideChainFunc(uintptr_t self);

void Parent_finalOverrideFunc(uintptr_t self);

void Parent_finalOverrideHopFunc(uintptr_t self);

void Parent_hopFunc(uintptr_t self);

int32_t Parent_nonoverride(uintptr_t self);

uintptr_t Parent_objectFunc__TypesOfArguments__overrides_Child__(uintptr_t self, uintptr_t arg);

uintptr_t Parent_objectOptionalFunc__TypesOfArguments__overrides_Child__(uintptr_t self, uintptr_t arg);

uintptr_t Parent_objectOptionalVar_get(uintptr_t self);

uintptr_t Parent_objectVar_get(uintptr_t self);

void Parent_overrideChainFunc(uintptr_t self);

int32_t Parent_primitiveTypeFunc__TypesOfArguments__Swift_Int32__(uintptr_t self, int32_t arg);

int32_t Parent_primitiveTypeVar_get(uintptr_t self);

uintptr_t Parent_subtypeObjectFunc__TypesOfArguments__overrides_Child__(uintptr_t self, uintptr_t arg);

uintptr_t Parent_subtypeObjectVar_get(uintptr_t self);

uintptr_t Parent_subtypeOptionalObjectFunc(uintptr_t self);

uintptr_t Parent_subtypeOptionalObjectVar_get(uintptr_t self);

NSNumber * Parent_subtypeOptionalPrimitiveFunc(uintptr_t self);

NSNumber * Parent_subtypeOptionalPrimitiveVar_get(uintptr_t self);

uintptr_t __root___Child_init_allocate();

void __root___Child_init_initialize__TypesOfArguments__Swift_UInt__(uintptr_t __kt);

uintptr_t __root___GrandChild_init_allocate();

void __root___GrandChild_init_initialize__TypesOfArguments__Swift_UInt__(uintptr_t __kt);

uintptr_t __root___Parent_init_allocate();

void __root___Parent_init_initialize__TypesOfArguments__Swift_UInt__(uintptr_t __kt);

