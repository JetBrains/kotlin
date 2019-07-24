import java.util.Comparator
import java.util.stream.Collectors

internal class Test {
    fun main(lst: List<Int>) {
        val newLst: List<Int> = /*before list*/lst/*after list*/.stream/*before stream*/()/* after stream*/
                .filter { x: Int -> x > 10 }
                .map { x: Int -> x + 2 }/*some comment*/.distinct/*another comment*/()/* one more comment */.sorted()/*another one comment*/
                .sorted(Comparator.naturalOrder())
                .peek { x: Int -> println(x) }.limit(1)
                .skip(42)/*skipped*/
                /*collecting one*/.collect/*collecting two */(Collectors.toList())/* cool */
    }
}