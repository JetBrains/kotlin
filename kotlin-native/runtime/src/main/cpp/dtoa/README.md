This is code for handling conversions between floating point and strings.
Adapted from Apache Harmony.
The code is split between C++ and Kotlin. This is the C++ part.
For kotlin part look in `kotlin-native/runtime/src/main/kotlin/kotlin/native/internal` for
`NumberConverter.kt` and `FloatingPointParser.kt`

Original source code: https://github.com/apache/harmony/tree/trunk/classlib/modules/luni/src/main/native/luni/shared
For `hycomp.h` it's https://github.com/apache/harmony/blob/trunk/classlib/modules/portlib/src/main/native/include/shared/hycomp.h

The only relevant change is: JNI is replaced with K/N runtime-specific interop.
