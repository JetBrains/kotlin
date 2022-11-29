import kotlinx.cinterop.*
import kt54284.*
import kotlin.test.*

fun main() {
    assertEquals(KFILE, "FILE:__FILE__")
    assertEquals(KLINE, "LINE:__LINE__")
    assertEquals(KTIME, "TIME:__TIME__")
    assertEquals(KDATE, "DATE:__DATE__")
    assertEquals(KFILENAME, "NAME:__FILE_NAME__")
    assertEquals(KBASEFILE, "BASE:__BASE_FILE__")
}
