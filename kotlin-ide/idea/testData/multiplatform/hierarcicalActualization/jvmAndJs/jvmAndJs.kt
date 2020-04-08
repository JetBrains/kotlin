package foo

actual class ExpectInCommonActualInMiddle

expect class ExpectInMiddleActualInPlatforms

expect class <!NO_ACTUAL_FOR_EXPECT("class 'ExpectInMiddleWithoutActual'", "js for JS", ""), NO_ACTUAL_FOR_EXPECT("class 'ExpectInMiddleWithoutActual'", "jvm for JVM", "")!>ExpectInMiddleWithoutActual<!>
