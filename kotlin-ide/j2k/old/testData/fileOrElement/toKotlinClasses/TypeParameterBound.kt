import java.util.*

internal interface I<T : List<Iterator<String>>>

internal class C : I<ArrayList<Iterator<String>>>
