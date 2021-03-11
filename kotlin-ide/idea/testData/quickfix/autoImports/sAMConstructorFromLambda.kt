// "Import" "true"
import java.util.Collections
import java.util.ArrayList

fun foo() {
    Collections.sort(
            ArrayList<Int>(),
            <caret>Comparator { x: Int, y: Int -> x - y }
    )
}