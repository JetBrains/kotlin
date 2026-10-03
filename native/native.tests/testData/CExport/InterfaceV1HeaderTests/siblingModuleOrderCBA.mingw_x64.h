#ifndef KONAN_SIBLINGMODULEORDERCBA_H
#define KONAN_SIBLINGMODULEORDERCBA_H
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
typedef bool            siblingModuleOrderCBA_KBoolean;
#else
typedef _Bool           siblingModuleOrderCBA_KBoolean;
#endif
typedef unsigned short     siblingModuleOrderCBA_KChar;
typedef signed char        siblingModuleOrderCBA_KByte;
typedef short              siblingModuleOrderCBA_KShort;
typedef int                siblingModuleOrderCBA_KInt;
typedef long long          siblingModuleOrderCBA_KLong;
typedef unsigned char      siblingModuleOrderCBA_KUByte;
typedef unsigned short     siblingModuleOrderCBA_KUShort;
typedef unsigned int       siblingModuleOrderCBA_KUInt;
typedef unsigned long long siblingModuleOrderCBA_KULong;
typedef float              siblingModuleOrderCBA_KFloat;
typedef double             siblingModuleOrderCBA_KDouble;
#ifndef _MSC_VER
typedef float __attribute__ ((__vector_size__ (16))) siblingModuleOrderCBA_KVector128;
#else
#include <xmmintrin.h>
typedef __m128 siblingModuleOrderCBA_KVector128;
#endif
typedef void*              siblingModuleOrderCBA_KNativePtr;
struct siblingModuleOrderCBA_KType;
typedef struct siblingModuleOrderCBA_KType siblingModuleOrderCBA_KType;

typedef struct {
  siblingModuleOrderCBA_KNativePtr pinned;
} siblingModuleOrderCBA_kref_kotlin_Byte;
typedef struct {
  siblingModuleOrderCBA_KNativePtr pinned;
} siblingModuleOrderCBA_kref_kotlin_Short;
typedef struct {
  siblingModuleOrderCBA_KNativePtr pinned;
} siblingModuleOrderCBA_kref_kotlin_Int;
typedef struct {
  siblingModuleOrderCBA_KNativePtr pinned;
} siblingModuleOrderCBA_kref_kotlin_Long;
typedef struct {
  siblingModuleOrderCBA_KNativePtr pinned;
} siblingModuleOrderCBA_kref_kotlin_Float;
typedef struct {
  siblingModuleOrderCBA_KNativePtr pinned;
} siblingModuleOrderCBA_kref_kotlin_Double;
typedef struct {
  siblingModuleOrderCBA_KNativePtr pinned;
} siblingModuleOrderCBA_kref_kotlin_Char;
typedef struct {
  siblingModuleOrderCBA_KNativePtr pinned;
} siblingModuleOrderCBA_kref_kotlin_Boolean;
typedef struct {
  siblingModuleOrderCBA_KNativePtr pinned;
} siblingModuleOrderCBA_kref_kotlin_Unit;
typedef struct {
  siblingModuleOrderCBA_KNativePtr pinned;
} siblingModuleOrderCBA_kref_kotlin_UByte;
typedef struct {
  siblingModuleOrderCBA_KNativePtr pinned;
} siblingModuleOrderCBA_kref_kotlin_UShort;
typedef struct {
  siblingModuleOrderCBA_KNativePtr pinned;
} siblingModuleOrderCBA_kref_kotlin_UInt;
typedef struct {
  siblingModuleOrderCBA_KNativePtr pinned;
} siblingModuleOrderCBA_kref_kotlin_ULong;
typedef struct {
  siblingModuleOrderCBA_KNativePtr pinned;
} siblingModuleOrderCBA_kref_foo_AlphaFromA;
typedef struct {
  siblingModuleOrderCBA_KNativePtr pinned;
} siblingModuleOrderCBA_kref_foo_GammaFromC;
typedef struct {
  siblingModuleOrderCBA_KNativePtr pinned;
} siblingModuleOrderCBA_kref_foo_BetaFromB;


typedef struct {
  /* Service functions. */
  void (*DisposeStablePointer)(siblingModuleOrderCBA_KNativePtr ptr);
  void (*DisposeString)(const char* string);
  siblingModuleOrderCBA_KBoolean (*IsInstance)(siblingModuleOrderCBA_KNativePtr ref, const siblingModuleOrderCBA_KType* type);
  siblingModuleOrderCBA_kref_kotlin_Byte (*createNullableByte)(siblingModuleOrderCBA_KByte);
  siblingModuleOrderCBA_KByte (*getNonNullValueOfByte)(siblingModuleOrderCBA_kref_kotlin_Byte);
  siblingModuleOrderCBA_kref_kotlin_Short (*createNullableShort)(siblingModuleOrderCBA_KShort);
  siblingModuleOrderCBA_KShort (*getNonNullValueOfShort)(siblingModuleOrderCBA_kref_kotlin_Short);
  siblingModuleOrderCBA_kref_kotlin_Int (*createNullableInt)(siblingModuleOrderCBA_KInt);
  siblingModuleOrderCBA_KInt (*getNonNullValueOfInt)(siblingModuleOrderCBA_kref_kotlin_Int);
  siblingModuleOrderCBA_kref_kotlin_Long (*createNullableLong)(siblingModuleOrderCBA_KLong);
  siblingModuleOrderCBA_KLong (*getNonNullValueOfLong)(siblingModuleOrderCBA_kref_kotlin_Long);
  siblingModuleOrderCBA_kref_kotlin_Float (*createNullableFloat)(siblingModuleOrderCBA_KFloat);
  siblingModuleOrderCBA_KFloat (*getNonNullValueOfFloat)(siblingModuleOrderCBA_kref_kotlin_Float);
  siblingModuleOrderCBA_kref_kotlin_Double (*createNullableDouble)(siblingModuleOrderCBA_KDouble);
  siblingModuleOrderCBA_KDouble (*getNonNullValueOfDouble)(siblingModuleOrderCBA_kref_kotlin_Double);
  siblingModuleOrderCBA_kref_kotlin_Char (*createNullableChar)(siblingModuleOrderCBA_KChar);
  siblingModuleOrderCBA_KChar (*getNonNullValueOfChar)(siblingModuleOrderCBA_kref_kotlin_Char);
  siblingModuleOrderCBA_kref_kotlin_Boolean (*createNullableBoolean)(siblingModuleOrderCBA_KBoolean);
  siblingModuleOrderCBA_KBoolean (*getNonNullValueOfBoolean)(siblingModuleOrderCBA_kref_kotlin_Boolean);
  siblingModuleOrderCBA_kref_kotlin_Unit (*createNullableUnit)(void);
  siblingModuleOrderCBA_kref_kotlin_UByte (*createNullableUByte)(siblingModuleOrderCBA_KUByte);
  siblingModuleOrderCBA_KUByte (*getNonNullValueOfUByte)(siblingModuleOrderCBA_kref_kotlin_UByte);
  siblingModuleOrderCBA_kref_kotlin_UShort (*createNullableUShort)(siblingModuleOrderCBA_KUShort);
  siblingModuleOrderCBA_KUShort (*getNonNullValueOfUShort)(siblingModuleOrderCBA_kref_kotlin_UShort);
  siblingModuleOrderCBA_kref_kotlin_UInt (*createNullableUInt)(siblingModuleOrderCBA_KUInt);
  siblingModuleOrderCBA_KUInt (*getNonNullValueOfUInt)(siblingModuleOrderCBA_kref_kotlin_UInt);
  siblingModuleOrderCBA_kref_kotlin_ULong (*createNullableULong)(siblingModuleOrderCBA_KULong);
  siblingModuleOrderCBA_KULong (*getNonNullValueOfULong)(siblingModuleOrderCBA_kref_kotlin_ULong);

  /* User functions. */
  struct {
    struct {
      struct {
        struct {
          siblingModuleOrderCBA_KType* (*_type)(void);
          siblingModuleOrderCBA_kref_foo_AlphaFromA (*AlphaFromA)();
          siblingModuleOrderCBA_KInt (*fromA)(siblingModuleOrderCBA_kref_foo_AlphaFromA thiz);
        } AlphaFromA;
        struct {
          siblingModuleOrderCBA_KType* (*_type)(void);
          siblingModuleOrderCBA_kref_foo_GammaFromC (*GammaFromC)();
          siblingModuleOrderCBA_KInt (*useAlpha)(siblingModuleOrderCBA_kref_foo_GammaFromC thiz);
        } GammaFromC;
        struct {
          siblingModuleOrderCBA_KType* (*_type)(void);
          siblingModuleOrderCBA_kref_foo_BetaFromB (*BetaFromB)();
          siblingModuleOrderCBA_KInt (*useAlpha)(siblingModuleOrderCBA_kref_foo_BetaFromB thiz);
        } BetaFromB;
      } foo;
    } root;
  } kotlin;
} siblingModuleOrderCBA_ExportedSymbols;
extern siblingModuleOrderCBA_ExportedSymbols* siblingModuleOrderCBA_symbols(void);
#ifdef __cplusplus
}  /* extern "C" */
#endif
#endif  /* KONAN_SIBLINGMODULEORDERCBA_H */
