#include <Foundation/Foundation.h>
#include <stdint.h>

void AbstractBase_abstractFun1(uintptr_t self);

void AbstractBase_abstractFun2(uintptr_t self);

int32_t AbstractBase_abstractVal_get(uintptr_t self);

void AbstractDerived2_abstractFun1(uintptr_t self);

void Child_actuallyOverride__TypesOfArguments__Swift_Int32_opt__overrides_Parent_overrides_Parent_opt___(uintptr_t self, NSNumber * nullable, uintptr_t poly, uintptr_t nullablePoly);

void Child_finalOverrideFunc(uintptr_t self);

NSArray<id> * Child_genericReturnTypeFunc(uintptr_t self);

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

void OpenDerived1_abstractFun1(uintptr_t self);

void OpenDerived1_abstractFun2(uintptr_t self);

int32_t OpenDerived1_abstractVal_get(uintptr_t self);

void Parent_actuallyOverride__TypesOfArguments__Swift_Int32_overrides_Child_overrides_Child__(uintptr_t self, int32_t nullable, uintptr_t poly, uintptr_t nullablePoly);

void Parent_finalOverrideFunc(uintptr_t self);

void Parent_finalOverrideHopFunc(uintptr_t self);

NSArray<id> * Parent_genericReturnTypeFunc(uintptr_t self);

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

NSString * Parent_value_get(uintptr_t self);

uintptr_t __root___Child_init_allocate();

void __root___Child_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(uintptr_t __kt, int32_t value);

void __root___Child_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32_overrides_Parent_overrides_Parent__(uintptr_t __kt, int32_t nullable, uintptr_t poly, uintptr_t nullablePoly);

void __root___Child_init_initialize__TypesOfArguments__Swift_UInt_Swift_String__(uintptr_t __kt, NSString * value);

uintptr_t __root___GrandChild_init_allocate();

void __root___GrandChild_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(uintptr_t __kt, int32_t value);

uintptr_t __root___OpenDerived1_init_allocate();

void __root___OpenDerived1_init_initialize__TypesOfArguments__Swift_UInt__(uintptr_t __kt);

void __root___OpenDerived1_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(uintptr_t __kt, int32_t x);

uintptr_t __root___Parent_init_allocate();

void __root___Parent_init_initialize__TypesOfArguments__Swift_UInt_Swift_String__(uintptr_t __kt, NSString * value);

