// FILE: ValueExample.java

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

// FILE: test.kt

import kotlin.test.assertEquals

fun box(): String {
    val obj = ValueExample("name", 12, 4.5)
    assertEquals("name", obj.getName())
    assertEquals("name", obj.name)
    assertEquals(12, obj.getAge())
    assertEquals(12, obj.age)
    assertEquals(4.5, obj.score)

    val ex: ValueExample.Exercise<Int> = ValueExample.Exercise.of("nam1e", 42)
    assertEquals("nam1e", ex.name)
    assertEquals(42, ex.value)
    return "OK"
}
