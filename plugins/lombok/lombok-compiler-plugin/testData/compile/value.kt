//FILE: ValueExample.java

import lombok.*;

@Value public class ValueExample {
    private final String name;
    private int age;
    private double score;

    @ToString(includeFieldNames=true)
    @Value(staticConstructor="of")
    public static class Exercise<T> {
        private final String name;
        private T value;
    }

    public static void usage() {
        val obj = new ValueExample("name", 12, 4.5);
        obj.getName();
        obj.getAge();
        obj.getScore();

        Exercise<Integer> ex = Exercise.of("name", 12);
        ex.getName();
        ex.getValue();
    }
}

//FILE: test.kt

class Test {
    fun run() {
        val obj = ValueExample("name", 12, 4.5)
        obj.getName()
        val name = obj.name
        obj.getAge()
        val age = obj.age
        val score = obj.score

        val ex: ValueExample.Exercise<Int> = ValueExample.Exercise.of("name", 12)
    }
}
