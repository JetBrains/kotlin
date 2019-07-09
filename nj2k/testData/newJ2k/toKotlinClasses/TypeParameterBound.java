//file
import java.util.*;

interface I<T extends List<Iterator<String>>> {
}

class C implements I<ArrayList<Iterator<String>>> {
}
