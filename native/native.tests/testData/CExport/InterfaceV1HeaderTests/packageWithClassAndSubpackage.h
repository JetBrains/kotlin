#ifndef KONAN_PACKAGEWITHCLASSANDSUBPACKAGE_H
#define KONAN_PACKAGEWITHCLASSANDSUBPACKAGE_H
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
typedef bool            packageWithClassAndSubpackage_KBoolean;
#else
typedef _Bool           packageWithClassAndSubpackage_KBoolean;
#endif
typedef unsigned short     packageWithClassAndSubpackage_KChar;
typedef signed char        packageWithClassAndSubpackage_KByte;
typedef short              packageWithClassAndSubpackage_KShort;
typedef int                packageWithClassAndSubpackage_KInt;
typedef long long          packageWithClassAndSubpackage_KLong;
typedef unsigned char      packageWithClassAndSubpackage_KUByte;
typedef unsigned short     packageWithClassAndSubpackage_KUShort;
typedef unsigned int       packageWithClassAndSubpackage_KUInt;
typedef unsigned long long packageWithClassAndSubpackage_KULong;
typedef float              packageWithClassAndSubpackage_KFloat;
typedef double             packageWithClassAndSubpackage_KDouble;
typedef float __attribute__ ((__vector_size__ (16))) packageWithClassAndSubpackage_KVector128;
typedef void*              packageWithClassAndSubpackage_KNativePtr;
struct packageWithClassAndSubpackage_KType;
typedef struct packageWithClassAndSubpackage_KType packageWithClassAndSubpackage_KType;

typedef struct {
  packageWithClassAndSubpackage_KNativePtr pinned;
} packageWithClassAndSubpackage_kref_kotlin_Byte;
typedef struct {
  packageWithClassAndSubpackage_KNativePtr pinned;
} packageWithClassAndSubpackage_kref_kotlin_Short;
typedef struct {
  packageWithClassAndSubpackage_KNativePtr pinned;
} packageWithClassAndSubpackage_kref_kotlin_Int;
typedef struct {
  packageWithClassAndSubpackage_KNativePtr pinned;
} packageWithClassAndSubpackage_kref_kotlin_Long;
typedef struct {
  packageWithClassAndSubpackage_KNativePtr pinned;
} packageWithClassAndSubpackage_kref_kotlin_Float;
typedef struct {
  packageWithClassAndSubpackage_KNativePtr pinned;
} packageWithClassAndSubpackage_kref_kotlin_Double;
typedef struct {
  packageWithClassAndSubpackage_KNativePtr pinned;
} packageWithClassAndSubpackage_kref_kotlin_Char;
typedef struct {
  packageWithClassAndSubpackage_KNativePtr pinned;
} packageWithClassAndSubpackage_kref_kotlin_Boolean;
typedef struct {
  packageWithClassAndSubpackage_KNativePtr pinned;
} packageWithClassAndSubpackage_kref_kotlin_Unit;
typedef struct {
  packageWithClassAndSubpackage_KNativePtr pinned;
} packageWithClassAndSubpackage_kref_kotlin_UByte;
typedef struct {
  packageWithClassAndSubpackage_KNativePtr pinned;
} packageWithClassAndSubpackage_kref_kotlin_UShort;
typedef struct {
  packageWithClassAndSubpackage_KNativePtr pinned;
} packageWithClassAndSubpackage_kref_kotlin_UInt;
typedef struct {
  packageWithClassAndSubpackage_KNativePtr pinned;
} packageWithClassAndSubpackage_kref_kotlin_ULong;
typedef struct {
  packageWithClassAndSubpackage_KNativePtr pinned;
} packageWithClassAndSubpackage_kref_foo_Bar;
typedef struct {
  packageWithClassAndSubpackage_KNativePtr pinned;
} packageWithClassAndSubpackage_kref_foo_sub_sub2_Baz;
typedef struct {
  packageWithClassAndSubpackage_KNativePtr pinned;
} packageWithClassAndSubpackage_kref_foo_Bar2;


typedef struct {
  /* Service functions. */
  void (*DisposeStablePointer)(packageWithClassAndSubpackage_KNativePtr ptr);
  void (*DisposeString)(const char* string);
  packageWithClassAndSubpackage_KBoolean (*IsInstance)(packageWithClassAndSubpackage_KNativePtr ref, const packageWithClassAndSubpackage_KType* type);
  packageWithClassAndSubpackage_kref_kotlin_Byte (*createNullableByte)(packageWithClassAndSubpackage_KByte);
  packageWithClassAndSubpackage_KByte (*getNonNullValueOfByte)(packageWithClassAndSubpackage_kref_kotlin_Byte);
  packageWithClassAndSubpackage_kref_kotlin_Short (*createNullableShort)(packageWithClassAndSubpackage_KShort);
  packageWithClassAndSubpackage_KShort (*getNonNullValueOfShort)(packageWithClassAndSubpackage_kref_kotlin_Short);
  packageWithClassAndSubpackage_kref_kotlin_Int (*createNullableInt)(packageWithClassAndSubpackage_KInt);
  packageWithClassAndSubpackage_KInt (*getNonNullValueOfInt)(packageWithClassAndSubpackage_kref_kotlin_Int);
  packageWithClassAndSubpackage_kref_kotlin_Long (*createNullableLong)(packageWithClassAndSubpackage_KLong);
  packageWithClassAndSubpackage_KLong (*getNonNullValueOfLong)(packageWithClassAndSubpackage_kref_kotlin_Long);
  packageWithClassAndSubpackage_kref_kotlin_Float (*createNullableFloat)(packageWithClassAndSubpackage_KFloat);
  packageWithClassAndSubpackage_KFloat (*getNonNullValueOfFloat)(packageWithClassAndSubpackage_kref_kotlin_Float);
  packageWithClassAndSubpackage_kref_kotlin_Double (*createNullableDouble)(packageWithClassAndSubpackage_KDouble);
  packageWithClassAndSubpackage_KDouble (*getNonNullValueOfDouble)(packageWithClassAndSubpackage_kref_kotlin_Double);
  packageWithClassAndSubpackage_kref_kotlin_Char (*createNullableChar)(packageWithClassAndSubpackage_KChar);
  packageWithClassAndSubpackage_KChar (*getNonNullValueOfChar)(packageWithClassAndSubpackage_kref_kotlin_Char);
  packageWithClassAndSubpackage_kref_kotlin_Boolean (*createNullableBoolean)(packageWithClassAndSubpackage_KBoolean);
  packageWithClassAndSubpackage_KBoolean (*getNonNullValueOfBoolean)(packageWithClassAndSubpackage_kref_kotlin_Boolean);
  packageWithClassAndSubpackage_kref_kotlin_Unit (*createNullableUnit)(void);
  packageWithClassAndSubpackage_kref_kotlin_UByte (*createNullableUByte)(packageWithClassAndSubpackage_KUByte);
  packageWithClassAndSubpackage_KUByte (*getNonNullValueOfUByte)(packageWithClassAndSubpackage_kref_kotlin_UByte);
  packageWithClassAndSubpackage_kref_kotlin_UShort (*createNullableUShort)(packageWithClassAndSubpackage_KUShort);
  packageWithClassAndSubpackage_KUShort (*getNonNullValueOfUShort)(packageWithClassAndSubpackage_kref_kotlin_UShort);
  packageWithClassAndSubpackage_kref_kotlin_UInt (*createNullableUInt)(packageWithClassAndSubpackage_KUInt);
  packageWithClassAndSubpackage_KUInt (*getNonNullValueOfUInt)(packageWithClassAndSubpackage_kref_kotlin_UInt);
  packageWithClassAndSubpackage_kref_kotlin_ULong (*createNullableULong)(packageWithClassAndSubpackage_KULong);
  packageWithClassAndSubpackage_KULong (*getNonNullValueOfULong)(packageWithClassAndSubpackage_kref_kotlin_ULong);

  /* User functions. */
  struct {
    struct {
      struct {
        struct {
          packageWithClassAndSubpackage_KType* (*_type)(void);
          packageWithClassAndSubpackage_kref_foo_Bar (*Bar)();
        } Bar;
        struct {
          struct {
            struct {
              packageWithClassAndSubpackage_KType* (*_type)(void);
              packageWithClassAndSubpackage_kref_foo_sub_sub2_Baz (*Baz)();
            } Baz;
          } sub2;
        } sub;
        struct {
          packageWithClassAndSubpackage_KType* (*_type)(void);
          packageWithClassAndSubpackage_kref_foo_Bar2 (*Bar2)();
        } Bar2;
      } foo;
    } root;
  } kotlin;
} packageWithClassAndSubpackage_ExportedSymbols;
extern packageWithClassAndSubpackage_ExportedSymbols* packageWithClassAndSubpackage_symbols(void);
#ifdef __cplusplus
}  /* extern "C" */
#endif
#endif  /* KONAN_PACKAGEWITHCLASSANDSUBPACKAGE_H */
