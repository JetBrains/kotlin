#include <Foundation/Foundation.h>
#include <stdint.h>

NS_ASSUME_NONNULL_BEGIN

void * EnumSimple_FIRST();

void * EnumSimple_LAST();

void * EnumSimple_SECOND();

void * EnumWithAbstractMembers_MAGENTA();

void * EnumWithAbstractMembers_SKY();

void * EnumWithAbstractMembers_YELLOW();

int32_t EnumWithAbstractMembers_blue(void * self);

int32_t EnumWithAbstractMembers_green(void * self);

int32_t EnumWithAbstractMembers_ordinalSquare(void * self);

int32_t EnumWithAbstractMembers_red_get(void * self);

void * EnumWithMembers_NORTH();

void * EnumWithMembers_SOUTH();

NSString * EnumWithMembers_foo(void * self);

_Bool EnumWithMembers_isNorth_get(void * self);

void * Enum_a();

void * Enum_b();

int32_t Enum_i_get(void * self);

void Enum_i_set__TypesOfArguments__Swift_Int32__(void * self, int32_t newValue);

NSString * Enum_print(void * self);

void * __root___enumId__TypesOfArguments__ExportedKotlinPackages_kotlin_Enum__(void * e);

void * __root___ewamValues();

void * __root___yellow();

NS_ASSUME_NONNULL_END
