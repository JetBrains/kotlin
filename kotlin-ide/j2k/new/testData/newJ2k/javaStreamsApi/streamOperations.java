// RUNTIME_WITH_FULL_JDK

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Test {
    public void main(List<Integer> lst) {
        List<Integer> newLst = lst.stream()
                .filter(x -> x > 10)
                .map(x -> x + 2)
                .distinct()
                .sorted()
                .sorted(Comparator.<Integer>naturalOrder())
                .peek(x-> System.out.println(x))
                .limit(1)
                .skip(42)
                .collect(Collectors.toList());
    }
}