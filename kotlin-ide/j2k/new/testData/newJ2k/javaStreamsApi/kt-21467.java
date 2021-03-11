// RUNTIME_WITH_FULL_JDK

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Test {
    public void main() {
        List<String> activities = Stream.of("12")
                .map(v -> v + "nya")
                .filter(v -> v != null)
                .flatMap(v -> Stream.of(v)
                        .flatMap(s -> Stream.of(s))
                ).filter(v -> {
                    String name = v.getClass().getName();
                    if (name.equals("name")) {
                        return false;
                    }
                    return !name.equals("other_name");
                })
                .collect(Collectors.toList());
    }
}