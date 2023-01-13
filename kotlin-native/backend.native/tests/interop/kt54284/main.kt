import kotlinx.cinterop.*
import kt54284.*
import kotlin.test.*

fun main() {
    assertEquals("FILE:__FILE__", KFILE)
    assertEquals("LINE:__LINE__", KLINE)
    assertEquals("TIME:__TIME__", KTIME)
    assertEquals("DATE:__DATE__", KDATE)
    assertEquals("NAME:__FILE_NAME__", KFILENAME)
    assertEquals("BASE:__BASE_FILE__", KBASEFILE)
}
