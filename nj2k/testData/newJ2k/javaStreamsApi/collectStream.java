// RUNTIME_WITH_FULL_JDK

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class Test {
    public void main(List<String> lst) {
        List<String> toList = lst.stream().collect(Collectors.toList());
        Set<String> toSet = lst.stream().collect(Collectors.toSet());
        long count = lst.stream().count();
        boolean anyMatch = lst.stream().anyMatch(v -> v.isEmpty());
        boolean allMatch = lst.stream().allMatch(v -> v.isEmpty());
        boolean noneMatch = lst.stream().noneMatch(v -> v.isEmpty());
        lst.stream().forEach(v -> System.out.println(v));
    }
}