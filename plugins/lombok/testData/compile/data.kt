//FILE: DataExample.java

import lombok.*;

@Data public class DataExample {
    private final String name;
    @Setter(AccessLevel.PACKAGE) private int age;
    private double score;
    private String[] tags;

    @ToString(includeFieldNames=true)
    @Data(staticConstructor="of")
    public static class Exercise<T> {
        private final String name;
        private final T value;
    }

    public static void usage() {
        val obj = new DataExample("name");
        obj.getName();
        obj.getTags();
        obj.setScore(1.5);

        Exercise<Integer> ex = Exercise.of("name", 12);
    }
}

//FILE: test.kt

class Test {
    fun run() {
        val obj = DataExample("name")
        obj.getName()
        assertEquals(obj.name, "name")
        obj.getTags()
        val tags = obj.tags
        obj.setScore(1.5)
        assertEquals(obj.score, 1.5)
        obj.score = 2.5
        assertEquals(obj.score, 2.5)

        val ex: DataExample.Exercise<Int> = DataExample.Exercise.of("name", 12)
    }
}
