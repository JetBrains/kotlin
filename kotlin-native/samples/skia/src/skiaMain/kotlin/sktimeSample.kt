import kotlinx.cinterop.*
import SkTime.*

fun main() {
    println("Skia sample")
    println("${SkTime.GetNSecs()}")
    println("${SkTime.GetNSecs()}")
    println("${SkTime.GetNSecs()}")
    test_GetDateTime()
}

fun test_GetDateTime() {
    memScoped {
        val dateTime = alloc<SkTime__DateTime>()
        SkTime.GetDateTime(dateTime.ptr)
        println("${dateTime.fYear}-${dateTime.fMonth}-${dateTime.fDay} ${dateTime.fHour}:${dateTime.fMinute}.${dateTime.fSecond}" )
    }

}