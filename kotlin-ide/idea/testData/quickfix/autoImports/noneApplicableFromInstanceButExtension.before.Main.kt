// "Import" "true"
// ERROR: None of the following functions can be called with the arguments supplied: <br>public final fun buz(i: Int, l: Int): Unit defined in lib.Bar<br>public final fun buz(i: Int, m: M): Unit defined in lib.Bar
import lib.Bar

fun useSite() {

    val bar = Bar()
    bar.buz<caret>("1", "2")
}