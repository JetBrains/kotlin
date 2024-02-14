// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: kt54284_fmodules.def
compilerOpts = -fmodules
---
#define KFILE "FILE:" __FILE__
#define KLINE "LINE:" __LINE__
#define KTIME "TIME:" __TIME__
#define KDATE "DATE:" __DATE__
#define KFILENAME "NAME:" __FILE_NAME__
#define KBASEFILE "BASE:" __BASE_FILE__


// MODULE: main(cinterop)
// FILE: main.kt
import kotlinx.cinterop.*
import kt54284_fmodules.*
import kotlin.test.*

@ExperimentalForeignApi
fun box(): String {
    assertEquals("FILE:__FILE__", KFILE)
    assertEquals("LINE:__LINE__", KLINE)
    assertEquals("TIME:__TIME__", KTIME)
    assertEquals("DATE:__DATE__", KDATE)
    assertEquals("NAME:__FILE_NAME__", KFILENAME)
    assertEquals("BASE:__BASE_FILE__", KBASEFILE)

    return "OK"
}
