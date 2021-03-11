package test

fun <!LINE_MARKER!>main<!>() {
    val e = Expect()

    e.platformFun()
    e.commonFun()

    topLevelPlatformFun()
    topLevelCommonFun()
}