#ifndef KONAN_INDEPENDENTMODULEORDER_H
#define KONAN_INDEPENDENTMODULEORDER_H
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
typedef bool            independentModuleOrder_KBoolean;
#else
typedef _Bool           independentModuleOrder_KBoolean;
#endif
typedef unsigned short     independentModuleOrder_KChar;
typedef signed char        independentModuleOrder_KByte;
typedef short              independentModuleOrder_KShort;
typedef int                independentModuleOrder_KInt;
typedef long long          independentModuleOrder_KLong;
typedef unsigned char      independentModuleOrder_KUByte;
typedef unsigned short     independentModuleOrder_KUShort;
typedef unsigned int       independentModuleOrder_KUInt;
typedef unsigned long long independentModuleOrder_KULong;
typedef float              independentModuleOrder_KFloat;
typedef double             independentModuleOrder_KDouble;
typedef float __attribute__ ((__vector_size__ (16))) independentModuleOrder_KVector128;
typedef void*              independentModuleOrder_KNativePtr;
struct independentModuleOrder_KType;
typedef struct independentModuleOrder_KType independentModuleOrder_KType;

typedef struct {
  independentModuleOrder_KNativePtr pinned;
} independentModuleOrder_kref_kotlin_Byte;
typedef struct {
  independentModuleOrder_KNativePtr pinned;
} independentModuleOrder_kref_kotlin_Short;
typedef struct {
  independentModuleOrder_KNativePtr pinned;
} independentModuleOrder_kref_kotlin_Int;
typedef struct {
  independentModuleOrder_KNativePtr pinned;
} independentModuleOrder_kref_kotlin_Long;
typedef struct {
  independentModuleOrder_KNativePtr pinned;
} independentModuleOrder_kref_kotlin_Float;
typedef struct {
  independentModuleOrder_KNativePtr pinned;
} independentModuleOrder_kref_kotlin_Double;
typedef struct {
  independentModuleOrder_KNativePtr pinned;
} independentModuleOrder_kref_kotlin_Char;
typedef struct {
  independentModuleOrder_KNativePtr pinned;
} independentModuleOrder_kref_kotlin_Boolean;
typedef struct {
  independentModuleOrder_KNativePtr pinned;
} independentModuleOrder_kref_kotlin_Unit;
typedef struct {
  independentModuleOrder_KNativePtr pinned;
} independentModuleOrder_kref_kotlin_UByte;
typedef struct {
  independentModuleOrder_KNativePtr pinned;
} independentModuleOrder_kref_kotlin_UShort;
typedef struct {
  independentModuleOrder_KNativePtr pinned;
} independentModuleOrder_kref_kotlin_UInt;
typedef struct {
  independentModuleOrder_KNativePtr pinned;
} independentModuleOrder_kref_kotlin_ULong;
typedef struct {
  independentModuleOrder_KNativePtr pinned;
} independentModuleOrder_kref_foo_Lib1Type;
typedef struct {
  independentModuleOrder_KNativePtr pinned;
} independentModuleOrder_kref_foo_Lib2Type;
typedef struct {
  independentModuleOrder_KNativePtr pinned;
} independentModuleOrder_kref_foo_Lib3Type;
typedef struct {
  independentModuleOrder_KNativePtr pinned;
} independentModuleOrder_kref_foo_Lib4Type;
typedef struct {
  independentModuleOrder_KNativePtr pinned;
} independentModuleOrder_kref_foo_Lib5Type;
typedef struct {
  independentModuleOrder_KNativePtr pinned;
} independentModuleOrder_kref_foo_Lib6Type;
typedef struct {
  independentModuleOrder_KNativePtr pinned;
} independentModuleOrder_kref_foo_Lib7Type;
typedef struct {
  independentModuleOrder_KNativePtr pinned;
} independentModuleOrder_kref_foo_Lib8Type;


typedef struct {
  /* Service functions. */
  void (*DisposeStablePointer)(independentModuleOrder_KNativePtr ptr);
  void (*DisposeString)(const char* string);
  independentModuleOrder_KBoolean (*IsInstance)(independentModuleOrder_KNativePtr ref, const independentModuleOrder_KType* type);
  independentModuleOrder_kref_kotlin_Byte (*createNullableByte)(independentModuleOrder_KByte);
  independentModuleOrder_KByte (*getNonNullValueOfByte)(independentModuleOrder_kref_kotlin_Byte);
  independentModuleOrder_kref_kotlin_Short (*createNullableShort)(independentModuleOrder_KShort);
  independentModuleOrder_KShort (*getNonNullValueOfShort)(independentModuleOrder_kref_kotlin_Short);
  independentModuleOrder_kref_kotlin_Int (*createNullableInt)(independentModuleOrder_KInt);
  independentModuleOrder_KInt (*getNonNullValueOfInt)(independentModuleOrder_kref_kotlin_Int);
  independentModuleOrder_kref_kotlin_Long (*createNullableLong)(independentModuleOrder_KLong);
  independentModuleOrder_KLong (*getNonNullValueOfLong)(independentModuleOrder_kref_kotlin_Long);
  independentModuleOrder_kref_kotlin_Float (*createNullableFloat)(independentModuleOrder_KFloat);
  independentModuleOrder_KFloat (*getNonNullValueOfFloat)(independentModuleOrder_kref_kotlin_Float);
  independentModuleOrder_kref_kotlin_Double (*createNullableDouble)(independentModuleOrder_KDouble);
  independentModuleOrder_KDouble (*getNonNullValueOfDouble)(independentModuleOrder_kref_kotlin_Double);
  independentModuleOrder_kref_kotlin_Char (*createNullableChar)(independentModuleOrder_KChar);
  independentModuleOrder_KChar (*getNonNullValueOfChar)(independentModuleOrder_kref_kotlin_Char);
  independentModuleOrder_kref_kotlin_Boolean (*createNullableBoolean)(independentModuleOrder_KBoolean);
  independentModuleOrder_KBoolean (*getNonNullValueOfBoolean)(independentModuleOrder_kref_kotlin_Boolean);
  independentModuleOrder_kref_kotlin_Unit (*createNullableUnit)(void);
  independentModuleOrder_kref_kotlin_UByte (*createNullableUByte)(independentModuleOrder_KUByte);
  independentModuleOrder_KUByte (*getNonNullValueOfUByte)(independentModuleOrder_kref_kotlin_UByte);
  independentModuleOrder_kref_kotlin_UShort (*createNullableUShort)(independentModuleOrder_KUShort);
  independentModuleOrder_KUShort (*getNonNullValueOfUShort)(independentModuleOrder_kref_kotlin_UShort);
  independentModuleOrder_kref_kotlin_UInt (*createNullableUInt)(independentModuleOrder_KUInt);
  independentModuleOrder_KUInt (*getNonNullValueOfUInt)(independentModuleOrder_kref_kotlin_UInt);
  independentModuleOrder_kref_kotlin_ULong (*createNullableULong)(independentModuleOrder_KULong);
  independentModuleOrder_KULong (*getNonNullValueOfULong)(independentModuleOrder_kref_kotlin_ULong);

  /* User functions. */
  struct {
    struct {
      struct {
        independentModuleOrder_KInt (*hub)();
      } hub;
      struct {
        struct {
          independentModuleOrder_KType* (*_type)(void);
          independentModuleOrder_kref_foo_Lib1Type (*Lib1Type)(independentModuleOrder_KInt value);
          independentModuleOrder_KInt (*get_value)(independentModuleOrder_kref_foo_Lib1Type thiz);
        } Lib1Type;
        struct {
          independentModuleOrder_KType* (*_type)(void);
          independentModuleOrder_kref_foo_Lib2Type (*Lib2Type)(independentModuleOrder_KInt value);
          independentModuleOrder_KInt (*get_value)(independentModuleOrder_kref_foo_Lib2Type thiz);
        } Lib2Type;
        struct {
          independentModuleOrder_KType* (*_type)(void);
          independentModuleOrder_kref_foo_Lib3Type (*Lib3Type)(independentModuleOrder_KInt value);
          independentModuleOrder_KInt (*get_value)(independentModuleOrder_kref_foo_Lib3Type thiz);
        } Lib3Type;
        struct {
          independentModuleOrder_KType* (*_type)(void);
          independentModuleOrder_kref_foo_Lib4Type (*Lib4Type)(independentModuleOrder_KInt value);
          independentModuleOrder_KInt (*get_value)(independentModuleOrder_kref_foo_Lib4Type thiz);
        } Lib4Type;
        struct {
          independentModuleOrder_KType* (*_type)(void);
          independentModuleOrder_kref_foo_Lib5Type (*Lib5Type)(independentModuleOrder_KInt value);
          independentModuleOrder_KInt (*get_value)(independentModuleOrder_kref_foo_Lib5Type thiz);
        } Lib5Type;
        struct {
          independentModuleOrder_KType* (*_type)(void);
          independentModuleOrder_kref_foo_Lib6Type (*Lib6Type)(independentModuleOrder_KInt value);
          independentModuleOrder_KInt (*get_value)(independentModuleOrder_kref_foo_Lib6Type thiz);
        } Lib6Type;
        struct {
          independentModuleOrder_KType* (*_type)(void);
          independentModuleOrder_kref_foo_Lib7Type (*Lib7Type)(independentModuleOrder_KInt value);
          independentModuleOrder_KInt (*get_value)(independentModuleOrder_kref_foo_Lib7Type thiz);
        } Lib7Type;
        struct {
          independentModuleOrder_KType* (*_type)(void);
          independentModuleOrder_kref_foo_Lib8Type (*Lib8Type)(independentModuleOrder_KInt value);
          independentModuleOrder_KInt (*get_value)(independentModuleOrder_kref_foo_Lib8Type thiz);
        } Lib8Type;
      } foo;
    } root;
  } kotlin;
} independentModuleOrder_ExportedSymbols;
extern independentModuleOrder_ExportedSymbols* independentModuleOrder_symbols(void);
#ifdef __cplusplus
}  /* extern "C" */
#endif
#endif  /* KONAN_INDEPENDENTMODULEORDER_H */
