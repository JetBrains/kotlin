// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

// In an optimized binary linked against static caches the class checks are built on the class
// hierarchy intervals that only the final link assigns, so instead of the compile-time constant
// intervals of a cache-less optimized build (IsSubclassFast) they go through IsSubclassFastIndirect.
// FILECHECK tests do not run with the user-libraries caches at all (the infrastructure skips them),
// the distribution's stdlib cache is enough to switch the compilation to the cached mode.

// DISABLE_NATIVE: optimizationMode=DEBUG
// DISABLE_NATIVE: optimizationMode=NO
// DISABLE_NATIVE: optimizationMode=OPT && cacheMode=NO

interface Marker
open class Base
class Middle : Base(), Marker

// CHECK-OPT: call{{.*}}@IsSubclassFastIndirect(
fun isBase(any: Any): Boolean = any is Base

// CHECK-OPT: call{{.*}}@IsSubclassFastIndirect(
fun isMiddle(any: Any): Boolean = any is Middle

fun box(): String {
    val middle = Middle()
    check(isBase(middle) && isMiddle(middle))
    check(!isMiddle(Base()))

    return "OK"
}
