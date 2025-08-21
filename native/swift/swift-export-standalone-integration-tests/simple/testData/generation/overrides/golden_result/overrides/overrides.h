#include <Foundation/Foundation.h>
#include <stdint.h>

NS_ASSUME_NONNULL_BEGIN

void AbstractBase_abstractFun1(void * self);

void AbstractBase_abstractFun2(void * self);

int32_t AbstractBase_abstractVal_get(void * self);

void AbstractDerived2_abstractFun1(void * self);

void Child_actuallyOverride__TypesOfArguments__Swift_Optional_Swift_Int32__overrides_Parent_Swift_Optional_overrides_Parent___(void * self, NSNumber * _Nullable nullable, void * poly, void * _Nullable nullablePoly);

_Bool Child_contains__TypesOfArguments__Swift_Int32__(void * self, int32_t element);

_Bool Child_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(void * self, void * _Nullable to);

void Child_finalOverrideFunc(void * self);

NSArray<id> * Child_genericReturnTypeFunc(void * self);

void Child_nonoverride(void * self) __attribute((noreturn));

void * Child_objectFunc__TypesOfArguments__overrides_Child__(void * self, void * arg);

void * _Nullable Child_objectOptionalFunc__TypesOfArguments__overrides_Child__(void * self, void * arg);

void * _Nullable Child_objectOptionalVar_get(void * self);

void * Child_objectVar_get(void * self);

void Child_overrideChainFunc(void * self);

int32_t Child_primitiveTypeFunc__TypesOfArguments__Swift_Int32__(void * self, int32_t arg);

int32_t Child_primitiveTypeVar_get(void * self);

void * Child_subtypeObjectFunc__TypesOfArguments__overrides_Child__(void * self, void * arg);

void * Child_subtypeObjectVar_get(void * self);

void * Child_subtypeOptionalObjectFunc(void * self);

void * Child_subtypeOptionalObjectVar_get(void * self);

int32_t Child_subtypeOptionalPrimitiveFunc(void * self);

int32_t Child_subtypeOptionalPrimitiveVar_get(void * self);

void GrandChild_finalOverrideHopFunc(void * self);

void GrandChild_hopFunc(void * self);

void GrandChild_overrideChainFunc(void * self);

void OpenDerived1_abstractFun1(void * self);

void OpenDerived1_abstractFun2(void * self);

int32_t OpenDerived1_abstractVal_get(void * self);

void Parent_actuallyOverride__TypesOfArguments__Swift_Int32_overrides_Child_overrides_Child__(void * self, int32_t nullable, void * poly, void * nullablePoly);

_Bool Parent_contains__TypesOfArguments__Swift_Int32__(void * self, int32_t element);

_Bool Parent_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(void * self, void * _Nullable to);

void Parent_finalOverrideFunc(void * self);

void Parent_finalOverrideHopFunc(void * self);

NSArray<id> * Parent_genericReturnTypeFunc(void * self);

void Parent_hopFunc(void * self);

int32_t Parent_nonoverride(void * self);

void * Parent_objectFunc__TypesOfArguments__overrides_Child__(void * self, void * arg);

void * _Nullable Parent_objectOptionalFunc__TypesOfArguments__overrides_Child__(void * self, void * arg);

void * _Nullable Parent_objectOptionalVar_get(void * self);

void * Parent_objectVar_get(void * self);

void Parent_overrideChainFunc(void * self);

int32_t Parent_primitiveTypeFunc__TypesOfArguments__Swift_Int32__(void * self, int32_t arg);

int32_t Parent_primitiveTypeVar_get(void * self);

void * Parent_subtypeObjectFunc__TypesOfArguments__overrides_Child__(void * self, void * arg);

void * Parent_subtypeObjectVar_get(void * self);

void * _Nullable Parent_subtypeOptionalObjectFunc(void * self);

void * _Nullable Parent_subtypeOptionalObjectVar_get(void * self);

NSNumber * _Nullable Parent_subtypeOptionalPrimitiveFunc(void * self);

NSNumber * _Nullable Parent_subtypeOptionalPrimitiveVar_get(void * self);

NSString * Parent_value_get(void * self);

void * __root___Child_init_allocate();

void __root___Child_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(void * __kt, int32_t value);

void __root___Child_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_overrides_Parent_overrides_Parent__(void * __kt, int32_t nullable, void * poly, void * nullablePoly);

void __root___Child_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(void * __kt, NSString * value);

void * __root___GrandChild_init_allocate();

void __root___GrandChild_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(void * __kt, int32_t value);

void * __root___OpenDerived1_init_allocate();

void __root___OpenDerived1_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(void * __kt);

void __root___OpenDerived1_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(void * __kt, int32_t x);

void * __root___Parent_init_allocate();

void __root___Parent_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(void * __kt, NSString * value);

NS_ASSUME_NONNULL_END
