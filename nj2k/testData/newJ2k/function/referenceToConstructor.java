// RUNTIME_WITH_FULL_JDK

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestLambda {
    public static void main(String[] args) {
        List<String> names = Arrays.asList("A", "B");
        List<Person> people = names.stream().map(Person::new).collect(Collectors.toList());
    }
}