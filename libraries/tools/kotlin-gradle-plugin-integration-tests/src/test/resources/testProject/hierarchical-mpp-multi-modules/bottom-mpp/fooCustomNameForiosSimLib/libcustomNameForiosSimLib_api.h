#ifndef KONAN_LIBCUSTOMNAMEFORIOSSIMLIB_H
#define KONAN_LIBCUSTOMNAMEFORIOSSIMLIB_H
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
typedef bool            libcustomNameForiosSimLib_KBoolean;
#else
typedef _Bool           libcustomNameForiosSimLib_KBoolean;
#endif
typedef unsigned short     libcustomNameForiosSimLib_KChar;
typedef signed char        libcustomNameForiosSimLib_KByte;
typedef short              libcustomNameForiosSimLib_KShort;
typedef int                libcustomNameForiosSimLib_KInt;
typedef long long          libcustomNameForiosSimLib_KLong;
typedef unsigned char      libcustomNameForiosSimLib_KUByte;
typedef unsigned short     libcustomNameForiosSimLib_KUShort;
typedef unsigned int       libcustomNameForiosSimLib_KUInt;
typedef unsigned long long libcustomNameForiosSimLib_KULong;
typedef float              libcustomNameForiosSimLib_KFloat;
typedef double             libcustomNameForiosSimLib_KDouble;
typedef float __attribute__ ((__vector_size__ (16))) libcustomNameForiosSimLib_KVector128;
typedef void*              libcustomNameForiosSimLib_KNativePtr;
struct libcustomNameForiosSimLib_KType;
typedef struct libcustomNameForiosSimLib_KType libcustomNameForiosSimLib_KType;

typedef struct {
  libcustomNameForiosSimLib_KNativePtr pinned;
} libcustomNameForiosSimLib_kref_kotlin_Byte;
typedef struct {
  libcustomNameForiosSimLib_KNativePtr pinned;
} libcustomNameForiosSimLib_kref_kotlin_Short;
typedef struct {
  libcustomNameForiosSimLib_KNativePtr pinned;
} libcustomNameForiosSimLib_kref_kotlin_Int;
typedef struct {
  libcustomNameForiosSimLib_KNativePtr pinned;
} libcustomNameForiosSimLib_kref_kotlin_Long;
typedef struct {
  libcustomNameForiosSimLib_KNativePtr pinned;
} libcustomNameForiosSimLib_kref_kotlin_Float;
typedef struct {
  libcustomNameForiosSimLib_KNativePtr pinned;
} libcustomNameForiosSimLib_kref_kotlin_Double;
typedef struct {
  libcustomNameForiosSimLib_KNativePtr pinned;
} libcustomNameForiosSimLib_kref_kotlin_Char;
typedef struct {
  libcustomNameForiosSimLib_KNativePtr pinned;
} libcustomNameForiosSimLib_kref_kotlin_Boolean;
typedef struct {
  libcustomNameForiosSimLib_KNativePtr pinned;
} libcustomNameForiosSimLib_kref_kotlin_Unit;
typedef struct {
  libcustomNameForiosSimLib_KNativePtr pinned;
} libcustomNameForiosSimLib_kref_transitiveStory_midActual_commonSource_SomeMPPInTheCommon;
typedef struct {
  libcustomNameForiosSimLib_KNativePtr pinned;
} libcustomNameForiosSimLib_kref_transitiveStory_midActual_commonSource_SomeMPPInTheCommon_Compainon;
typedef struct {
  libcustomNameForiosSimLib_KNativePtr pinned;
} libcustomNameForiosSimLib_kref_transitiveStory_midActual_sourceCalls_intemediateCall_BottomActualCommonInheritor;
typedef struct {
  libcustomNameForiosSimLib_KNativePtr pinned;
} libcustomNameForiosSimLib_kref_transitiveStory_midActual_sourceCalls_intemediateCall_BottomActualMPPInheritor;
typedef struct {
  libcustomNameForiosSimLib_KNativePtr pinned;
} libcustomNameForiosSimLib_kref_transitiveStory_midActual_sourceCalls_intemediateCall_SecondModCaller;
typedef struct {
  libcustomNameForiosSimLib_KNativePtr pinned;
} libcustomNameForiosSimLib_kref_transitiveStory_bottomActual_intermediateSrc_IntermediateMPPClassInBottomActual;


typedef struct {
  /* Service functions. */
  void (*DisposeStablePointer)(libcustomNameForiosSimLib_KNativePtr ptr);
  void (*DisposeString)(const char* string);
  libcustomNameForiosSimLib_KBoolean (*IsInstance)(libcustomNameForiosSimLib_KNativePtr ref, const libcustomNameForiosSimLib_KType* type);
  libcustomNameForiosSimLib_kref_kotlin_Byte (*createNullableByte)(libcustomNameForiosSimLib_KByte);
  libcustomNameForiosSimLib_kref_kotlin_Short (*createNullableShort)(libcustomNameForiosSimLib_KShort);
  libcustomNameForiosSimLib_kref_kotlin_Int (*createNullableInt)(libcustomNameForiosSimLib_KInt);
  libcustomNameForiosSimLib_kref_kotlin_Long (*createNullableLong)(libcustomNameForiosSimLib_KLong);
  libcustomNameForiosSimLib_kref_kotlin_Float (*createNullableFloat)(libcustomNameForiosSimLib_KFloat);
  libcustomNameForiosSimLib_kref_kotlin_Double (*createNullableDouble)(libcustomNameForiosSimLib_KDouble);
  libcustomNameForiosSimLib_kref_kotlin_Char (*createNullableChar)(libcustomNameForiosSimLib_KChar);
  libcustomNameForiosSimLib_kref_kotlin_Boolean (*createNullableBoolean)(libcustomNameForiosSimLib_KBoolean);
  libcustomNameForiosSimLib_kref_kotlin_Unit (*createNullableUnit)(void);

  /* User functions. */
  struct {
    struct {
      struct {
        struct {
          struct {
            const char* (*get_moduleName)();
            const char* (*get_sourceSetName)();
            const char* (*regularTLfunInTheMidActualCommmon)(const char* s);
            struct {
              libcustomNameForiosSimLib_KType* (*_type)(void);
              libcustomNameForiosSimLib_kref_transitiveStory_midActual_commonSource_SomeMPPInTheCommon (*SomeMPPInTheCommon)();
              libcustomNameForiosSimLib_KInt (*get_simpleVal)(libcustomNameForiosSimLib_kref_transitiveStory_midActual_commonSource_SomeMPPInTheCommon thiz);
              struct {
                libcustomNameForiosSimLib_KType* (*_type)(void);
                libcustomNameForiosSimLib_kref_transitiveStory_midActual_commonSource_SomeMPPInTheCommon_Compainon (*_instance)();
                const char* (*get_inTheCompanionOfBottomActualDeclarations)(libcustomNameForiosSimLib_kref_transitiveStory_midActual_commonSource_SomeMPPInTheCommon_Compainon thiz);
              } Compainon;
            } SomeMPPInTheCommon;
          } commonSource;
          struct {
            struct {
              struct {
                libcustomNameForiosSimLib_KType* (*_type)(void);
                libcustomNameForiosSimLib_kref_transitiveStory_midActual_sourceCalls_intemediateCall_BottomActualCommonInheritor (*BottomActualCommonInheritor)();
              } BottomActualCommonInheritor;
              struct {
                libcustomNameForiosSimLib_KType* (*_type)(void);
                libcustomNameForiosSimLib_kref_transitiveStory_midActual_sourceCalls_intemediateCall_BottomActualMPPInheritor (*BottomActualMPPInheritor)();
              } BottomActualMPPInheritor;
              struct {
                libcustomNameForiosSimLib_KType* (*_type)(void);
                libcustomNameForiosSimLib_kref_transitiveStory_midActual_sourceCalls_intemediateCall_SecondModCaller (*SecondModCaller)();
                libcustomNameForiosSimLib_kref_transitiveStory_bottomActual_intermediateSrc_IntermediateMPPClassInBottomActual (*get_interCallFive)(libcustomNameForiosSimLib_kref_transitiveStory_midActual_sourceCalls_intemediateCall_SecondModCaller thiz);
                libcustomNameForiosSimLib_KInt (*get_interCallFour)(libcustomNameForiosSimLib_kref_transitiveStory_midActual_sourceCalls_intemediateCall_SecondModCaller thiz);
                const char* (*get_interCallOne)(libcustomNameForiosSimLib_kref_transitiveStory_midActual_sourceCalls_intemediateCall_SecondModCaller thiz);
                libcustomNameForiosSimLib_KInt (*get_interCallThree)(libcustomNameForiosSimLib_kref_transitiveStory_midActual_sourceCalls_intemediateCall_SecondModCaller thiz);
                const char* (*get_interCallTwo)(libcustomNameForiosSimLib_kref_transitiveStory_midActual_sourceCalls_intemediateCall_SecondModCaller thiz);
              } SecondModCaller;
            } intemediateCall;
          } sourceCalls;
        } midActual;
      } transitiveStory;
    } root;
  } kotlin;
} libcustomNameForiosSimLib_ExportedSymbols;
extern libcustomNameForiosSimLib_ExportedSymbols* libcustomNameForiosSimLib_symbols(void);
#ifdef __cplusplus
}  /* extern "C" */
#endif
#endif  /* KONAN_LIBCUSTOMNAMEFORIOSSIMLIB_H */
