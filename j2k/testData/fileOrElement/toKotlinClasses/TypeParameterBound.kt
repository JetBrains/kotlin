import java.util.*

trait I<T : List<Iterator<String>>>

class C : I<ArrayList<Iterator<String>>>
