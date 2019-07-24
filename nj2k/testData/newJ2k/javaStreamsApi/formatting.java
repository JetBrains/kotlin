// RUNTIME_WITH_FULL_JDK

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

class Test {
    public void main(List<Integer> lst) {
        List<Integer> newLst = /*before list*/lst/*after list*/./*before stream*/stream()/* after stream*/
                .filter(x -> x > 10)
                .map(x -> x + 2)/*some comment*/./*another comment*/distinct()/* one more comment */.sorted()/*another one comment*/
                .sorted(Comparator.<Integer>naturalOrder())
                .peek(x -> System.out.println(x)).limit(1)
                .skip(42)/*skipped*/
                /*collecting one*/./*collecting two */collect(Collectors.toList())/* cool */;
    }
}