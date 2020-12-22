package foo

actual class ExpectInCommonActualInPlatforms
actual class <!LINE_MARKER("descr='Has declaration in common module'")!>ExpectInMiddleActualInPlatforms<!>

expect class <!NO_ACTUAL_FOR_EXPECT!>ExpectInJvmWithoutActual<!>

expect class <!LINE_MARKER("descr='Has actuals in JVM'")!>ExpectInJvmActualInJvm<!>
actual class <!LINE_MARKER!>ExpectInJvmActualInJvm<!>
