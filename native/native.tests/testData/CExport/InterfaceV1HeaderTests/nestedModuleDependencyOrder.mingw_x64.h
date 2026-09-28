#ifndef KONAN_NESTEDMODULEDEPENDENCYORDER_H
#define KONAN_NESTEDMODULEDEPENDENCYORDER_H
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
typedef bool            nestedModuleDependencyOrder_KBoolean;
#else
typedef _Bool           nestedModuleDependencyOrder_KBoolean;
#endif
typedef unsigned short     nestedModuleDependencyOrder_KChar;
typedef signed char        nestedModuleDependencyOrder_KByte;
typedef short              nestedModuleDependencyOrder_KShort;
typedef int                nestedModuleDependencyOrder_KInt;
typedef long long          nestedModuleDependencyOrder_KLong;
typedef unsigned char      nestedModuleDependencyOrder_KUByte;
typedef unsigned short     nestedModuleDependencyOrder_KUShort;
typedef unsigned int       nestedModuleDependencyOrder_KUInt;
typedef unsigned long long nestedModuleDependencyOrder_KULong;
typedef float              nestedModuleDependencyOrder_KFloat;
typedef double             nestedModuleDependencyOrder_KDouble;
#ifndef _MSC_VER
typedef float __attribute__ ((__vector_size__ (16))) nestedModuleDependencyOrder_KVector128;
#else
#include <xmmintrin.h>
typedef __m128 nestedModuleDependencyOrder_KVector128;
#endif
typedef void*              nestedModuleDependencyOrder_KNativePtr;
struct nestedModuleDependencyOrder_KType;
typedef struct nestedModuleDependencyOrder_KType nestedModuleDependencyOrder_KType;

typedef struct {
  nestedModuleDependencyOrder_KNativePtr pinned;
} nestedModuleDependencyOrder_kref_kotlin_Byte;
typedef struct {
  nestedModuleDependencyOrder_KNativePtr pinned;
} nestedModuleDependencyOrder_kref_kotlin_Short;
typedef struct {
  nestedModuleDependencyOrder_KNativePtr pinned;
} nestedModuleDependencyOrder_kref_kotlin_Int;
typedef struct {
  nestedModuleDependencyOrder_KNativePtr pinned;
} nestedModuleDependencyOrder_kref_kotlin_Long;
typedef struct {
  nestedModuleDependencyOrder_KNativePtr pinned;
} nestedModuleDependencyOrder_kref_kotlin_Float;
typedef struct {
  nestedModuleDependencyOrder_KNativePtr pinned;
} nestedModuleDependencyOrder_kref_kotlin_Double;
typedef struct {
  nestedModuleDependencyOrder_KNativePtr pinned;
} nestedModuleDependencyOrder_kref_kotlin_Char;
typedef struct {
  nestedModuleDependencyOrder_KNativePtr pinned;
} nestedModuleDependencyOrder_kref_kotlin_Boolean;
typedef struct {
  nestedModuleDependencyOrder_KNativePtr pinned;
} nestedModuleDependencyOrder_kref_kotlin_Unit;
typedef struct {
  nestedModuleDependencyOrder_KNativePtr pinned;
} nestedModuleDependencyOrder_kref_kotlin_UByte;
typedef struct {
  nestedModuleDependencyOrder_KNativePtr pinned;
} nestedModuleDependencyOrder_kref_kotlin_UShort;
typedef struct {
  nestedModuleDependencyOrder_KNativePtr pinned;
} nestedModuleDependencyOrder_kref_kotlin_UInt;
typedef struct {
  nestedModuleDependencyOrder_KNativePtr pinned;
} nestedModuleDependencyOrder_kref_kotlin_ULong;
typedef struct {
  nestedModuleDependencyOrder_KNativePtr pinned;
} nestedModuleDependencyOrder_kref_foo_AlphaFromA;
typedef struct {
  nestedModuleDependencyOrder_KNativePtr pinned;
} nestedModuleDependencyOrder_kref_foo_bar_AlphaBarFromA;
typedef struct {
  nestedModuleDependencyOrder_KNativePtr pinned;
} nestedModuleDependencyOrder_kref_foo_bar_GammaBarFromC;
typedef struct {
  nestedModuleDependencyOrder_KNativePtr pinned;
} nestedModuleDependencyOrder_kref_foo_GammaFromC;


typedef struct {
  /* Service functions. */
  void (*DisposeStablePointer)(nestedModuleDependencyOrder_KNativePtr ptr);
  void (*DisposeString)(const char* string);
  nestedModuleDependencyOrder_KBoolean (*IsInstance)(nestedModuleDependencyOrder_KNativePtr ref, const nestedModuleDependencyOrder_KType* type);
  nestedModuleDependencyOrder_kref_kotlin_Byte (*createNullableByte)(nestedModuleDependencyOrder_KByte);
  nestedModuleDependencyOrder_KByte (*getNonNullValueOfByte)(nestedModuleDependencyOrder_kref_kotlin_Byte);
  nestedModuleDependencyOrder_kref_kotlin_Short (*createNullableShort)(nestedModuleDependencyOrder_KShort);
  nestedModuleDependencyOrder_KShort (*getNonNullValueOfShort)(nestedModuleDependencyOrder_kref_kotlin_Short);
  nestedModuleDependencyOrder_kref_kotlin_Int (*createNullableInt)(nestedModuleDependencyOrder_KInt);
  nestedModuleDependencyOrder_KInt (*getNonNullValueOfInt)(nestedModuleDependencyOrder_kref_kotlin_Int);
  nestedModuleDependencyOrder_kref_kotlin_Long (*createNullableLong)(nestedModuleDependencyOrder_KLong);
  nestedModuleDependencyOrder_KLong (*getNonNullValueOfLong)(nestedModuleDependencyOrder_kref_kotlin_Long);
  nestedModuleDependencyOrder_kref_kotlin_Float (*createNullableFloat)(nestedModuleDependencyOrder_KFloat);
  nestedModuleDependencyOrder_KFloat (*getNonNullValueOfFloat)(nestedModuleDependencyOrder_kref_kotlin_Float);
  nestedModuleDependencyOrder_kref_kotlin_Double (*createNullableDouble)(nestedModuleDependencyOrder_KDouble);
  nestedModuleDependencyOrder_KDouble (*getNonNullValueOfDouble)(nestedModuleDependencyOrder_kref_kotlin_Double);
  nestedModuleDependencyOrder_kref_kotlin_Char (*createNullableChar)(nestedModuleDependencyOrder_KChar);
  nestedModuleDependencyOrder_KChar (*getNonNullValueOfChar)(nestedModuleDependencyOrder_kref_kotlin_Char);
  nestedModuleDependencyOrder_kref_kotlin_Boolean (*createNullableBoolean)(nestedModuleDependencyOrder_KBoolean);
  nestedModuleDependencyOrder_KBoolean (*getNonNullValueOfBoolean)(nestedModuleDependencyOrder_kref_kotlin_Boolean);
  nestedModuleDependencyOrder_kref_kotlin_Unit (*createNullableUnit)(void);
  nestedModuleDependencyOrder_kref_kotlin_UByte (*createNullableUByte)(nestedModuleDependencyOrder_KUByte);
  nestedModuleDependencyOrder_KUByte (*getNonNullValueOfUByte)(nestedModuleDependencyOrder_kref_kotlin_UByte);
  nestedModuleDependencyOrder_kref_kotlin_UShort (*createNullableUShort)(nestedModuleDependencyOrder_KUShort);
  nestedModuleDependencyOrder_KUShort (*getNonNullValueOfUShort)(nestedModuleDependencyOrder_kref_kotlin_UShort);
  nestedModuleDependencyOrder_kref_kotlin_UInt (*createNullableUInt)(nestedModuleDependencyOrder_KUInt);
  nestedModuleDependencyOrder_KUInt (*getNonNullValueOfUInt)(nestedModuleDependencyOrder_kref_kotlin_UInt);
  nestedModuleDependencyOrder_kref_kotlin_ULong (*createNullableULong)(nestedModuleDependencyOrder_KULong);
  nestedModuleDependencyOrder_KULong (*getNonNullValueOfULong)(nestedModuleDependencyOrder_kref_kotlin_ULong);

  /* User functions. */
  struct {
    struct {
      struct {
        struct {
          nestedModuleDependencyOrder_KType* (*_type)(void);
          nestedModuleDependencyOrder_kref_foo_AlphaFromA (*AlphaFromA)();
          nestedModuleDependencyOrder_KInt (*fromA)(nestedModuleDependencyOrder_kref_foo_AlphaFromA thiz);
        } AlphaFromA;
        struct {
          struct {
            nestedModuleDependencyOrder_KType* (*_type)(void);
            nestedModuleDependencyOrder_kref_foo_bar_AlphaBarFromA (*AlphaBarFromA)();
            nestedModuleDependencyOrder_KInt (*barFromA)(nestedModuleDependencyOrder_kref_foo_bar_AlphaBarFromA thiz);
          } AlphaBarFromA;
          struct {
            nestedModuleDependencyOrder_KType* (*_type)(void);
            nestedModuleDependencyOrder_kref_foo_bar_GammaBarFromC (*GammaBarFromC)();
            nestedModuleDependencyOrder_KInt (*useAlphaBar)(nestedModuleDependencyOrder_kref_foo_bar_GammaBarFromC thiz);
          } GammaBarFromC;
        } bar;
        struct {
          nestedModuleDependencyOrder_KType* (*_type)(void);
          nestedModuleDependencyOrder_kref_foo_GammaFromC (*GammaFromC)();
          nestedModuleDependencyOrder_KInt (*useAlpha)(nestedModuleDependencyOrder_kref_foo_GammaFromC thiz);
        } GammaFromC;
      } foo;
    } root;
  } kotlin;
} nestedModuleDependencyOrder_ExportedSymbols;
extern nestedModuleDependencyOrder_ExportedSymbols* nestedModuleDependencyOrder_symbols(void);
#ifdef __cplusplus
}  /* extern "C" */
#endif
#endif  /* KONAN_NESTEDMODULEDEPENDENCYORDER_H */
