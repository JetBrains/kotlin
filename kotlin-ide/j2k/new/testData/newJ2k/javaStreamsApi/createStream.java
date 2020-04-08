// RUNTIME_WITH_FULL_JDK

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Test {
    public void main(List<String> lst) {
        List<String> streamOfList = lst.stream()
                .map(x -> x + "e")
                .collect(Collectors.toList());

        List<Integer> streamOfElements = Stream.of(1, 2, 3)
                .map(x -> x + 1)
                .collect(Collectors.toList());

        Integer[] array = {1, 2, 3};
        List<Integer> streamOfArray = Arrays.stream(array)
                .map(x -> x + 1)
                .collect(Collectors.toList());

        List<Integer> streamOfArray2 = Stream.of(array)
                .map(x -> x + 1)
                .collect(Collectors.toList());

        List<Integer> streamIterate = Stream.iterate(2, v -> v * 2)
                .map(x -> x + 1)
                .collect(Collectors.toList());

        List<Integer> streamGenerate = Stream.generate(() -> 42)
                .map(x -> x + 1)
                .collect(Collectors.toList());

    }
}