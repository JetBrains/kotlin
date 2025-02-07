#ifndef KONAN_SMOKE_H
#define KONAN_SMOKE_H
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
typedef bool            smoke_KBoolean;
#else
typedef _Bool           smoke_KBoolean;
#endif
typedef unsigned short     smoke_KChar;
typedef signed char        smoke_KByte;
typedef short              smoke_KShort;
typedef int                smoke_KInt;
typedef long long          smoke_KLong;
typedef unsigned char      smoke_KUByte;
typedef unsigned short     smoke_KUShort;
typedef unsigned int       smoke_KUInt;
typedef unsigned long long smoke_KULong;
typedef float              smoke_KFloat;
typedef double             smoke_KDouble;
#ifndef _MSC_VER
typedef float __attribute__ ((__vector_size__ (16))) smoke_KVector128;
#else
#include <xmmintrin.h>
typedef __m128 smoke_KVector128;
#endif
typedef void*              smoke_KNativePtr;
struct smoke_KType;
typedef struct smoke_KType smoke_KType;

typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_kotlin_Byte;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_kotlin_Short;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_kotlin_Int;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_kotlin_Long;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_kotlin_Float;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_kotlin_Double;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_kotlin_Char;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_kotlin_Boolean;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_kotlin_Unit;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_kotlin_UByte;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_kotlin_UShort;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_kotlin_UInt;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_kotlin_ULong;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_kotlin_Any;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_tests_native_CatInterface;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_tests_native_Tom;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_tests_native_SimpleClass;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_tests_native_DataClass;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_tests_native_MarkerInterface;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_tests_native_MyEnum;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_tests_native_MyEnum_A;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_tests_native_MyEnum_B;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_tests_native_MyEnum_C;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_tests_native_SealedClass;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_tests_native_SealedClass_A;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_tests_native_SealedClass_B;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_tests_native_SealedClass_C;
typedef struct {
  smoke_KNativePtr pinned;
} smoke_kref_tests_native_SealedClass_C_D;


typedef struct {
  /* Service functions. */
  void (*DisposeStablePointer)(smoke_KNativePtr ptr);
  void (*DisposeString)(const char* string);
  smoke_KBoolean (*IsInstance)(smoke_KNativePtr ref, const smoke_KType* type);
  smoke_kref_kotlin_Byte (*createNullableByte)(smoke_KByte);
  smoke_KByte (*getNonNullValueOfByte)(smoke_kref_kotlin_Byte);
  smoke_kref_kotlin_Short (*createNullableShort)(smoke_KShort);
  smoke_KShort (*getNonNullValueOfShort)(smoke_kref_kotlin_Short);
  smoke_kref_kotlin_Int (*createNullableInt)(smoke_KInt);
  smoke_KInt (*getNonNullValueOfInt)(smoke_kref_kotlin_Int);
  smoke_kref_kotlin_Long (*createNullableLong)(smoke_KLong);
  smoke_KLong (*getNonNullValueOfLong)(smoke_kref_kotlin_Long);
  smoke_kref_kotlin_Float (*createNullableFloat)(smoke_KFloat);
  smoke_KFloat (*getNonNullValueOfFloat)(smoke_kref_kotlin_Float);
  smoke_kref_kotlin_Double (*createNullableDouble)(smoke_KDouble);
  smoke_KDouble (*getNonNullValueOfDouble)(smoke_kref_kotlin_Double);
  smoke_kref_kotlin_Char (*createNullableChar)(smoke_KChar);
  smoke_KChar (*getNonNullValueOfChar)(smoke_kref_kotlin_Char);
  smoke_kref_kotlin_Boolean (*createNullableBoolean)(smoke_KBoolean);
  smoke_KBoolean (*getNonNullValueOfBoolean)(smoke_kref_kotlin_Boolean);
  smoke_kref_kotlin_Unit (*createNullableUnit)(void);
  smoke_kref_kotlin_UByte (*createNullableUByte)(smoke_KUByte);
  smoke_KUByte (*getNonNullValueOfUByte)(smoke_kref_kotlin_UByte);
  smoke_kref_kotlin_UShort (*createNullableUShort)(smoke_KUShort);
  smoke_KUShort (*getNonNullValueOfUShort)(smoke_kref_kotlin_UShort);
  smoke_kref_kotlin_UInt (*createNullableUInt)(smoke_KUInt);
  smoke_KUInt (*getNonNullValueOfUInt)(smoke_kref_kotlin_UInt);
  smoke_kref_kotlin_ULong (*createNullableULong)(smoke_KULong);
  smoke_KULong (*getNonNullValueOfULong)(smoke_kref_kotlin_ULong);

  /* User functions. */
  struct {
    struct {
      struct {
        struct {
          struct {
            smoke_KType* (*_type)(void);
            smoke_kref_tests_native_SimpleClass (*SimpleClass)();
          } SimpleClass;
          struct {
            smoke_KType* (*_type)(void);
            smoke_kref_tests_native_DataClass (*DataClass)(smoke_kref_tests_native_SimpleClass a, smoke_KInt b);
            smoke_kref_tests_native_SimpleClass (*get_a)(smoke_kref_tests_native_DataClass thiz);
            smoke_KInt (*get_b)(smoke_kref_tests_native_DataClass thiz);
            void (*set_b)(smoke_kref_tests_native_DataClass thiz, smoke_KInt set);
            smoke_kref_tests_native_SimpleClass (*component1)(smoke_kref_tests_native_DataClass thiz);
            smoke_KInt (*component2)(smoke_kref_tests_native_DataClass thiz);
            smoke_kref_tests_native_DataClass (*copy)(smoke_kref_tests_native_DataClass thiz, smoke_kref_tests_native_SimpleClass a, smoke_KInt b);
            smoke_KBoolean (*equals)(smoke_kref_tests_native_DataClass thiz, smoke_kref_kotlin_Any other);
            smoke_KInt (*hashCode)(smoke_kref_tests_native_DataClass thiz);
            const char* (*toString)(smoke_kref_tests_native_DataClass thiz);
          } DataClass;
          struct {
            smoke_KType* (*_type)(void);
          } MarkerInterface;
          struct {
            smoke_KType* (*_type)(void);
            const char* (*meow)(smoke_kref_tests_native_CatInterface thiz);
          } CatInterface;
          struct {
            smoke_KType* (*_type)(void);
            smoke_kref_tests_native_Tom (*Tom)();
            const char* (*meow)(smoke_kref_tests_native_Tom thiz);
          } Tom;
          struct {
            struct {
              smoke_kref_tests_native_MyEnum (*get)(); /* enum entry for A. */
            } A;
            struct {
              smoke_kref_tests_native_MyEnum (*get)(); /* enum entry for B. */
            } B;
            struct {
              smoke_kref_tests_native_MyEnum (*get)(); /* enum entry for C. */
            } C;
            smoke_KType* (*_type)(void);
          } MyEnum;
          struct {
            struct {
              smoke_KType* (*_type)(void);
              smoke_kref_tests_native_SealedClass_A (*A)();
            } A;
            struct {
              smoke_KType* (*_type)(void);
              smoke_kref_tests_native_SealedClass_B (*_instance)();
            } B;
            struct {
              struct {
                smoke_KType* (*_type)(void);
                smoke_kref_tests_native_SealedClass_C_D (*D)();
              } D;
              smoke_KType* (*_type)(void);
              smoke_kref_tests_native_SealedClass_C (*C)();
            } C;
            smoke_KType* (*_type)(void);
            smoke_kref_tests_native_SealedClass (*SealedClass)();
          } SealedClass;
          smoke_KDouble (*get_constDouble)();
          smoke_KFloat (*get_constFloat)();
          smoke_KInt (*get_constInt)();
          smoke_KLong (*get_constLong)();
          smoke_kref_kotlin_Any (*get_variableAnyNullable)();
          void (*set_variableAnyNullable)(smoke_kref_kotlin_Any set);
          const char* (*get_variableString)();
          void (*set_variableString)(const char* set);
          smoke_KInt (*consumeValueClass)(smoke_KInt param);
          const char* (*functionWithParams)(const char* a, const char* b);
          smoke_KInt (*produceValueClass)();
          void (*interfaceExtension)(smoke_kref_tests_native_CatInterface thiz);
          void (*interfaceExtension_)(smoke_kref_tests_native_Tom thiz);
        } native;
      } tests;
    } root;
  } kotlin;
} smoke_ExportedSymbols;
extern smoke_ExportedSymbols* smoke_symbols(void);
#ifdef __cplusplus
}  /* extern "C" */
#endif
#endif  /* KONAN_SMOKE_H */
