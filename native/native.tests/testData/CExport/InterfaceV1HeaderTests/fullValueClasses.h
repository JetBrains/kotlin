#ifndef KONAN_FULLVALUECLASSES_H
#define KONAN_FULLVALUECLASSES_H
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
typedef bool            fullValueClasses_KBoolean;
#else
typedef _Bool           fullValueClasses_KBoolean;
#endif
typedef unsigned short     fullValueClasses_KChar;
typedef signed char        fullValueClasses_KByte;
typedef short              fullValueClasses_KShort;
typedef int                fullValueClasses_KInt;
typedef long long          fullValueClasses_KLong;
typedef unsigned char      fullValueClasses_KUByte;
typedef unsigned short     fullValueClasses_KUShort;
typedef unsigned int       fullValueClasses_KUInt;
typedef unsigned long long fullValueClasses_KULong;
typedef float              fullValueClasses_KFloat;
typedef double             fullValueClasses_KDouble;
typedef float __attribute__ ((__vector_size__ (16))) fullValueClasses_KVector128;
typedef void*              fullValueClasses_KNativePtr;
struct fullValueClasses_KType;
typedef struct fullValueClasses_KType fullValueClasses_KType;

typedef struct {
  fullValueClasses_KNativePtr pinned;
} fullValueClasses_kref_kotlin_Byte;
typedef struct {
  fullValueClasses_KNativePtr pinned;
} fullValueClasses_kref_kotlin_Short;
typedef struct {
  fullValueClasses_KNativePtr pinned;
} fullValueClasses_kref_kotlin_Int;
typedef struct {
  fullValueClasses_KNativePtr pinned;
} fullValueClasses_kref_kotlin_Long;
typedef struct {
  fullValueClasses_KNativePtr pinned;
} fullValueClasses_kref_kotlin_Float;
typedef struct {
  fullValueClasses_KNativePtr pinned;
} fullValueClasses_kref_kotlin_Double;
typedef struct {
  fullValueClasses_KNativePtr pinned;
} fullValueClasses_kref_kotlin_Char;
typedef struct {
  fullValueClasses_KNativePtr pinned;
} fullValueClasses_kref_kotlin_Boolean;
typedef struct {
  fullValueClasses_KNativePtr pinned;
} fullValueClasses_kref_kotlin_Unit;
typedef struct {
  fullValueClasses_KNativePtr pinned;
} fullValueClasses_kref_kotlin_UByte;
typedef struct {
  fullValueClasses_KNativePtr pinned;
} fullValueClasses_kref_kotlin_UShort;
typedef struct {
  fullValueClasses_KNativePtr pinned;
} fullValueClasses_kref_kotlin_UInt;
typedef struct {
  fullValueClasses_KNativePtr pinned;
} fullValueClasses_kref_kotlin_ULong;
typedef struct {
  fullValueClasses_KNativePtr pinned;
} fullValueClasses_kref_tests_native_Point;
typedef struct {
  fullValueClasses_KNativePtr pinned;
} fullValueClasses_kref_kotlin_Any;


typedef struct {
  /* Service functions. */
  void (*DisposeStablePointer)(fullValueClasses_KNativePtr ptr);
  void (*DisposeString)(const char* string);
  fullValueClasses_KBoolean (*IsInstance)(fullValueClasses_KNativePtr ref, const fullValueClasses_KType* type);
  fullValueClasses_kref_kotlin_Byte (*createNullableByte)(fullValueClasses_KByte);
  fullValueClasses_KByte (*getNonNullValueOfByte)(fullValueClasses_kref_kotlin_Byte);
  fullValueClasses_kref_kotlin_Short (*createNullableShort)(fullValueClasses_KShort);
  fullValueClasses_KShort (*getNonNullValueOfShort)(fullValueClasses_kref_kotlin_Short);
  fullValueClasses_kref_kotlin_Int (*createNullableInt)(fullValueClasses_KInt);
  fullValueClasses_KInt (*getNonNullValueOfInt)(fullValueClasses_kref_kotlin_Int);
  fullValueClasses_kref_kotlin_Long (*createNullableLong)(fullValueClasses_KLong);
  fullValueClasses_KLong (*getNonNullValueOfLong)(fullValueClasses_kref_kotlin_Long);
  fullValueClasses_kref_kotlin_Float (*createNullableFloat)(fullValueClasses_KFloat);
  fullValueClasses_KFloat (*getNonNullValueOfFloat)(fullValueClasses_kref_kotlin_Float);
  fullValueClasses_kref_kotlin_Double (*createNullableDouble)(fullValueClasses_KDouble);
  fullValueClasses_KDouble (*getNonNullValueOfDouble)(fullValueClasses_kref_kotlin_Double);
  fullValueClasses_kref_kotlin_Char (*createNullableChar)(fullValueClasses_KChar);
  fullValueClasses_KChar (*getNonNullValueOfChar)(fullValueClasses_kref_kotlin_Char);
  fullValueClasses_kref_kotlin_Boolean (*createNullableBoolean)(fullValueClasses_KBoolean);
  fullValueClasses_KBoolean (*getNonNullValueOfBoolean)(fullValueClasses_kref_kotlin_Boolean);
  fullValueClasses_kref_kotlin_Unit (*createNullableUnit)(void);
  fullValueClasses_kref_kotlin_UByte (*createNullableUByte)(fullValueClasses_KUByte);
  fullValueClasses_KUByte (*getNonNullValueOfUByte)(fullValueClasses_kref_kotlin_UByte);
  fullValueClasses_kref_kotlin_UShort (*createNullableUShort)(fullValueClasses_KUShort);
  fullValueClasses_KUShort (*getNonNullValueOfUShort)(fullValueClasses_kref_kotlin_UShort);
  fullValueClasses_kref_kotlin_UInt (*createNullableUInt)(fullValueClasses_KUInt);
  fullValueClasses_KUInt (*getNonNullValueOfUInt)(fullValueClasses_kref_kotlin_UInt);
  fullValueClasses_kref_kotlin_ULong (*createNullableULong)(fullValueClasses_KULong);
  fullValueClasses_KULong (*getNonNullValueOfULong)(fullValueClasses_kref_kotlin_ULong);

  /* User functions. */
  struct {
    struct {
      struct {
        struct {
          struct {
            fullValueClasses_KType* (*_type)(void);
            fullValueClasses_kref_tests_native_Point (*Point)(fullValueClasses_KInt x, fullValueClasses_KInt y);
            fullValueClasses_KInt (*get_x)(fullValueClasses_kref_tests_native_Point thiz);
            fullValueClasses_KInt (*get_y)(fullValueClasses_kref_tests_native_Point thiz);
            fullValueClasses_KBoolean (*equals)(fullValueClasses_kref_tests_native_Point thiz, fullValueClasses_kref_kotlin_Any other);
            fullValueClasses_KInt (*hashCode)(fullValueClasses_kref_tests_native_Point thiz);
            const char* (*toString)(fullValueClasses_kref_tests_native_Point thiz);
          } Point;
          fullValueClasses_KInt (*sum)(fullValueClasses_kref_tests_native_Point point);
          fullValueClasses_KInt (*twice)(fullValueClasses_KInt meters);
        } native;
      } tests;
    } root;
  } kotlin;
} fullValueClasses_ExportedSymbols;
extern fullValueClasses_ExportedSymbols* fullValueClasses_symbols(void);
#ifdef __cplusplus
}  /* extern "C" */
#endif
#endif  /* KONAN_FULLVALUECLASSES_H */
