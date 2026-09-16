This is code for handling conversions between floating point and strings.
Adapted from Apache Harmony.
The code is rewritten from C++ to Kotlin. Implemented little-endian order.
We only have little-endian targets in Native and Wasm for now, so shouldn't be a problem.

Original source code: https://github.com/apache/harmony/tree/trunk/classlib/modules/luni/src/main/native/luni/shared
