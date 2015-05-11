import java.util.*

interface I<T : List<Iterator<String>>>

class C : I<ArrayList<Iterator<String>>>
