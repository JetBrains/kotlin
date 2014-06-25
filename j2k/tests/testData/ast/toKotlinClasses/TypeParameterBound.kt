import java.util.*
import kotlin.List
import kotlin.Iterator

trait I<T : List<Iterator<String>>>

class C : I<ArrayList<Iterator<String>>>
