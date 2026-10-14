#ifndef KONAN_MODULEDEPENDENCYORDER_H
#define KONAN_MODULEDEPENDENCYORDER_H
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
typedef bool            moduleDependencyOrder_KBoolean;
#else
typedef _Bool           moduleDependencyOrder_KBoolean;
#endif
typedef unsigned short     moduleDependencyOrder_KChar;
typedef signed char        moduleDependencyOrder_KByte;
typedef short              moduleDependencyOrder_KShort;
typedef int                moduleDependencyOrder_KInt;
typedef long long          moduleDependencyOrder_KLong;
typedef unsigned char      moduleDependencyOrder_KUByte;
typedef unsigned short     moduleDependencyOrder_KUShort;
typedef unsigned int       moduleDependencyOrder_KUInt;
typedef unsigned long long moduleDependencyOrder_KULong;
typedef float              moduleDependencyOrder_KFloat;
typedef double             moduleDependencyOrder_KDouble;
typedef float __attribute__ ((__vector_size__ (16))) moduleDependencyOrder_KVector128;
typedef void*              moduleDependencyOrder_KNativePtr;
struct moduleDependencyOrder_KType;
typedef struct moduleDependencyOrder_KType moduleDependencyOrder_KType;

typedef struct {
  moduleDependencyOrder_KNativePtr pinned;
} moduleDependencyOrder_kref_kotlin_Byte;
typedef struct {
  moduleDependencyOrder_KNativePtr pinned;
} moduleDependencyOrder_kref_kotlin_Short;
typedef struct {
  moduleDependencyOrder_KNativePtr pinned;
} moduleDependencyOrder_kref_kotlin_Int;
typedef struct {
  moduleDependencyOrder_KNativePtr pinned;
} moduleDependencyOrder_kref_kotlin_Long;
typedef struct {
  moduleDependencyOrder_KNativePtr pinned;
} moduleDependencyOrder_kref_kotlin_Float;
typedef struct {
  moduleDependencyOrder_KNativePtr pinned;
} moduleDependencyOrder_kref_kotlin_Double;
typedef struct {
  moduleDependencyOrder_KNativePtr pinned;
} moduleDependencyOrder_kref_kotlin_Char;
typedef struct {
  moduleDependencyOrder_KNativePtr pinned;
} moduleDependencyOrder_kref_kotlin_Boolean;
typedef struct {
  moduleDependencyOrder_KNativePtr pinned;
} moduleDependencyOrder_kref_kotlin_Unit;
typedef struct {
  moduleDependencyOrder_KNativePtr pinned;
} moduleDependencyOrder_kref_kotlin_UByte;
typedef struct {
  moduleDependencyOrder_KNativePtr pinned;
} moduleDependencyOrder_kref_kotlin_UShort;
typedef struct {
  moduleDependencyOrder_KNativePtr pinned;
} moduleDependencyOrder_kref_kotlin_UInt;
typedef struct {
  moduleDependencyOrder_KNativePtr pinned;
} moduleDependencyOrder_kref_kotlin_ULong;
typedef struct {
  moduleDependencyOrder_KNativePtr pinned;
} moduleDependencyOrder_kref_foo_AlphaFromA;
typedef struct {
  moduleDependencyOrder_KNativePtr pinned;
} moduleDependencyOrder_kref_foo_GammaFromC;


typedef struct {
  /* Service functions. */
  void (*DisposeStablePointer)(moduleDependencyOrder_KNativePtr ptr);
  void (*DisposeString)(const char* string);
  moduleDependencyOrder_KBoolean (*IsInstance)(moduleDependencyOrder_KNativePtr ref, const moduleDependencyOrder_KType* type);
  moduleDependencyOrder_kref_kotlin_Byte (*createNullableByte)(moduleDependencyOrder_KByte);
  moduleDependencyOrder_KByte (*getNonNullValueOfByte)(moduleDependencyOrder_kref_kotlin_Byte);
  moduleDependencyOrder_kref_kotlin_Short (*createNullableShort)(moduleDependencyOrder_KShort);
  moduleDependencyOrder_KShort (*getNonNullValueOfShort)(moduleDependencyOrder_kref_kotlin_Short);
  moduleDependencyOrder_kref_kotlin_Int (*createNullableInt)(moduleDependencyOrder_KInt);
  moduleDependencyOrder_KInt (*getNonNullValueOfInt)(moduleDependencyOrder_kref_kotlin_Int);
  moduleDependencyOrder_kref_kotlin_Long (*createNullableLong)(moduleDependencyOrder_KLong);
  moduleDependencyOrder_KLong (*getNonNullValueOfLong)(moduleDependencyOrder_kref_kotlin_Long);
  moduleDependencyOrder_kref_kotlin_Float (*createNullableFloat)(moduleDependencyOrder_KFloat);
  moduleDependencyOrder_KFloat (*getNonNullValueOfFloat)(moduleDependencyOrder_kref_kotlin_Float);
  moduleDependencyOrder_kref_kotlin_Double (*createNullableDouble)(moduleDependencyOrder_KDouble);
  moduleDependencyOrder_KDouble (*getNonNullValueOfDouble)(moduleDependencyOrder_kref_kotlin_Double);
  moduleDependencyOrder_kref_kotlin_Char (*createNullableChar)(moduleDependencyOrder_KChar);
  moduleDependencyOrder_KChar (*getNonNullValueOfChar)(moduleDependencyOrder_kref_kotlin_Char);
  moduleDependencyOrder_kref_kotlin_Boolean (*createNullableBoolean)(moduleDependencyOrder_KBoolean);
  moduleDependencyOrder_KBoolean (*getNonNullValueOfBoolean)(moduleDependencyOrder_kref_kotlin_Boolean);
  moduleDependencyOrder_kref_kotlin_Unit (*createNullableUnit)(void);
  moduleDependencyOrder_kref_kotlin_UByte (*createNullableUByte)(moduleDependencyOrder_KUByte);
  moduleDependencyOrder_KUByte (*getNonNullValueOfUByte)(moduleDependencyOrder_kref_kotlin_UByte);
  moduleDependencyOrder_kref_kotlin_UShort (*createNullableUShort)(moduleDependencyOrder_KUShort);
  moduleDependencyOrder_KUShort (*getNonNullValueOfUShort)(moduleDependencyOrder_kref_kotlin_UShort);
  moduleDependencyOrder_kref_kotlin_UInt (*createNullableUInt)(moduleDependencyOrder_KUInt);
  moduleDependencyOrder_KUInt (*getNonNullValueOfUInt)(moduleDependencyOrder_kref_kotlin_UInt);
  moduleDependencyOrder_kref_kotlin_ULong (*createNullableULong)(moduleDependencyOrder_KULong);
  moduleDependencyOrder_KULong (*getNonNullValueOfULong)(moduleDependencyOrder_kref_kotlin_ULong);

  /* User functions. */
  struct {
    struct {
      struct {
        struct {
          moduleDependencyOrder_KType* (*_type)(void);
          moduleDependencyOrder_kref_foo_AlphaFromA (*AlphaFromA)();
          moduleDependencyOrder_KInt (*fromA)(moduleDependencyOrder_kref_foo_AlphaFromA thiz);
        } AlphaFromA;
        struct {
          moduleDependencyOrder_KType* (*_type)(void);
          moduleDependencyOrder_kref_foo_GammaFromC (*GammaFromC)();
          moduleDependencyOrder_KInt (*useAlpha)(moduleDependencyOrder_kref_foo_GammaFromC thiz);
        } GammaFromC;
      } foo;
    } root;
  } kotlin;
} moduleDependencyOrder_ExportedSymbols;
extern moduleDependencyOrder_ExportedSymbols* moduleDependencyOrder_symbols(void);
#ifdef __cplusplus
}  /* extern "C" */
#endif
#endif  /* KONAN_MODULEDEPENDENCYORDER_H */
