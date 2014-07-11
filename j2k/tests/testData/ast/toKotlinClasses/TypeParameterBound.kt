import java.util.*
import kotlin.Iterator
import kotlin.List

trait I<T : List<Iterator<String>>>

class C : I<ArrayList<Iterator<String>>>
