#ifndef KONAN_SIBLINGMODULEORDERBCA_H
#define KONAN_SIBLINGMODULEORDERBCA_H
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
typedef bool            siblingModuleOrderBCA_KBoolean;
#else
typedef _Bool           siblingModuleOrderBCA_KBoolean;
#endif
typedef unsigned short     siblingModuleOrderBCA_KChar;
typedef signed char        siblingModuleOrderBCA_KByte;
typedef short              siblingModuleOrderBCA_KShort;
typedef int                siblingModuleOrderBCA_KInt;
typedef long long          siblingModuleOrderBCA_KLong;
typedef unsigned char      siblingModuleOrderBCA_KUByte;
typedef unsigned short     siblingModuleOrderBCA_KUShort;
typedef unsigned int       siblingModuleOrderBCA_KUInt;
typedef unsigned long long siblingModuleOrderBCA_KULong;
typedef float              siblingModuleOrderBCA_KFloat;
typedef double             siblingModuleOrderBCA_KDouble;
#ifndef _MSC_VER
typedef float __attribute__ ((__vector_size__ (16))) siblingModuleOrderBCA_KVector128;
#else
#include <xmmintrin.h>
typedef __m128 siblingModuleOrderBCA_KVector128;
#endif
typedef void*              siblingModuleOrderBCA_KNativePtr;
struct siblingModuleOrderBCA_KType;
typedef struct siblingModuleOrderBCA_KType siblingModuleOrderBCA_KType;

typedef struct {
  siblingModuleOrderBCA_KNativePtr pinned;
} siblingModuleOrderBCA_kref_kotlin_Byte;
typedef struct {
  siblingModuleOrderBCA_KNativePtr pinned;
} siblingModuleOrderBCA_kref_kotlin_Short;
typedef struct {
  siblingModuleOrderBCA_KNativePtr pinned;
} siblingModuleOrderBCA_kref_kotlin_Int;
typedef struct {
  siblingModuleOrderBCA_KNativePtr pinned;
} siblingModuleOrderBCA_kref_kotlin_Long;
typedef struct {
  siblingModuleOrderBCA_KNativePtr pinned;
} siblingModuleOrderBCA_kref_kotlin_Float;
typedef struct {
  siblingModuleOrderBCA_KNativePtr pinned;
} siblingModuleOrderBCA_kref_kotlin_Double;
typedef struct {
  siblingModuleOrderBCA_KNativePtr pinned;
} siblingModuleOrderBCA_kref_kotlin_Char;
typedef struct {
  siblingModuleOrderBCA_KNativePtr pinned;
} siblingModuleOrderBCA_kref_kotlin_Boolean;
typedef struct {
  siblingModuleOrderBCA_KNativePtr pinned;
} siblingModuleOrderBCA_kref_kotlin_Unit;
typedef struct {
  siblingModuleOrderBCA_KNativePtr pinned;
} siblingModuleOrderBCA_kref_kotlin_UByte;
typedef struct {
  siblingModuleOrderBCA_KNativePtr pinned;
} siblingModuleOrderBCA_kref_kotlin_UShort;
typedef struct {
  siblingModuleOrderBCA_KNativePtr pinned;
} siblingModuleOrderBCA_kref_kotlin_UInt;
typedef struct {
  siblingModuleOrderBCA_KNativePtr pinned;
} siblingModuleOrderBCA_kref_kotlin_ULong;
typedef struct {
  siblingModuleOrderBCA_KNativePtr pinned;
} siblingModuleOrderBCA_kref_foo_AlphaFromA;
typedef struct {
  siblingModuleOrderBCA_KNativePtr pinned;
} siblingModuleOrderBCA_kref_foo_BetaFromB;
typedef struct {
  siblingModuleOrderBCA_KNativePtr pinned;
} siblingModuleOrderBCA_kref_foo_GammaFromC;


typedef struct {
  /* Service functions. */
  void (*DisposeStablePointer)(siblingModuleOrderBCA_KNativePtr ptr);
  void (*DisposeString)(const char* string);
  siblingModuleOrderBCA_KBoolean (*IsInstance)(siblingModuleOrderBCA_KNativePtr ref, const siblingModuleOrderBCA_KType* type);
  siblingModuleOrderBCA_kref_kotlin_Byte (*createNullableByte)(siblingModuleOrderBCA_KByte);
  siblingModuleOrderBCA_KByte (*getNonNullValueOfByte)(siblingModuleOrderBCA_kref_kotlin_Byte);
  siblingModuleOrderBCA_kref_kotlin_Short (*createNullableShort)(siblingModuleOrderBCA_KShort);
  siblingModuleOrderBCA_KShort (*getNonNullValueOfShort)(siblingModuleOrderBCA_kref_kotlin_Short);
  siblingModuleOrderBCA_kref_kotlin_Int (*createNullableInt)(siblingModuleOrderBCA_KInt);
  siblingModuleOrderBCA_KInt (*getNonNullValueOfInt)(siblingModuleOrderBCA_kref_kotlin_Int);
  siblingModuleOrderBCA_kref_kotlin_Long (*createNullableLong)(siblingModuleOrderBCA_KLong);
  siblingModuleOrderBCA_KLong (*getNonNullValueOfLong)(siblingModuleOrderBCA_kref_kotlin_Long);
  siblingModuleOrderBCA_kref_kotlin_Float (*createNullableFloat)(siblingModuleOrderBCA_KFloat);
  siblingModuleOrderBCA_KFloat (*getNonNullValueOfFloat)(siblingModuleOrderBCA_kref_kotlin_Float);
  siblingModuleOrderBCA_kref_kotlin_Double (*createNullableDouble)(siblingModuleOrderBCA_KDouble);
  siblingModuleOrderBCA_KDouble (*getNonNullValueOfDouble)(siblingModuleOrderBCA_kref_kotlin_Double);
  siblingModuleOrderBCA_kref_kotlin_Char (*createNullableChar)(siblingModuleOrderBCA_KChar);
  siblingModuleOrderBCA_KChar (*getNonNullValueOfChar)(siblingModuleOrderBCA_kref_kotlin_Char);
  siblingModuleOrderBCA_kref_kotlin_Boolean (*createNullableBoolean)(siblingModuleOrderBCA_KBoolean);
  siblingModuleOrderBCA_KBoolean (*getNonNullValueOfBoolean)(siblingModuleOrderBCA_kref_kotlin_Boolean);
  siblingModuleOrderBCA_kref_kotlin_Unit (*createNullableUnit)(void);
  siblingModuleOrderBCA_kref_kotlin_UByte (*createNullableUByte)(siblingModuleOrderBCA_KUByte);
  siblingModuleOrderBCA_KUByte (*getNonNullValueOfUByte)(siblingModuleOrderBCA_kref_kotlin_UByte);
  siblingModuleOrderBCA_kref_kotlin_UShort (*createNullableUShort)(siblingModuleOrderBCA_KUShort);
  siblingModuleOrderBCA_KUShort (*getNonNullValueOfUShort)(siblingModuleOrderBCA_kref_kotlin_UShort);
  siblingModuleOrderBCA_kref_kotlin_UInt (*createNullableUInt)(siblingModuleOrderBCA_KUInt);
  siblingModuleOrderBCA_KUInt (*getNonNullValueOfUInt)(siblingModuleOrderBCA_kref_kotlin_UInt);
  siblingModuleOrderBCA_kref_kotlin_ULong (*createNullableULong)(siblingModuleOrderBCA_KULong);
  siblingModuleOrderBCA_KULong (*getNonNullValueOfULong)(siblingModuleOrderBCA_kref_kotlin_ULong);

  /* User functions. */
  struct {
    struct {
      struct {
        struct {
          siblingModuleOrderBCA_KType* (*_type)(void);
          siblingModuleOrderBCA_kref_foo_AlphaFromA (*AlphaFromA)();
          siblingModuleOrderBCA_KInt (*fromA)(siblingModuleOrderBCA_kref_foo_AlphaFromA thiz);
        } AlphaFromA;
        struct {
          siblingModuleOrderBCA_KType* (*_type)(void);
          siblingModuleOrderBCA_kref_foo_BetaFromB (*BetaFromB)();
          siblingModuleOrderBCA_KInt (*useAlpha)(siblingModuleOrderBCA_kref_foo_BetaFromB thiz);
        } BetaFromB;
        struct {
          siblingModuleOrderBCA_KType* (*_type)(void);
          siblingModuleOrderBCA_kref_foo_GammaFromC (*GammaFromC)();
          siblingModuleOrderBCA_KInt (*useAlpha)(siblingModuleOrderBCA_kref_foo_GammaFromC thiz);
        } GammaFromC;
      } foo;
    } root;
  } kotlin;
} siblingModuleOrderBCA_ExportedSymbols;
extern siblingModuleOrderBCA_ExportedSymbols* siblingModuleOrderBCA_symbols(void);
#ifdef __cplusplus
}  /* extern "C" */
#endif
#endif  /* KONAN_SIBLINGMODULEORDERBCA_H */
