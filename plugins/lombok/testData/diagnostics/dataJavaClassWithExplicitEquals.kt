// ISSUE: KT-90432
// FILE: Main.kt

fun main() {
    val p = Person()
    p.name = "Test"
    p == Person()
    p.hashCode()

    val q = PersonWithHashCode()
    q.name = "Test"
    q.hashCode()
}

// FILE: Person.java

import lombok.Data;

@Data
public class Person {
    private String name;
    private int age;

    @Override
    public boolean equals(Object o) {
        return this == o;
    }
}

// FILE: PersonWithHashCode.java

import lombok.Data;

@Data
public class PersonWithHashCode {
    private String name;

    @Override
    public int hashCode() {
        return 0;
    }
}
