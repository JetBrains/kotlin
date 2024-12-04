// FIR_IDENTICAL

// FILE: Base.java

import lombok.Data;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class Base {
    @Singular private java.util.List<String> names;
}

// FILE: Impl.java

import lombok.Data;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class Impl extends Base {
    @Singular private java.util.List<Integer> numbers;
}

// FILE: test.kt

fun test() {
    val base = Base.builder().name("name1").name("name2").build()
    val impl = Impl.builder().number(1).number(2).name("name1").name("name2").build()
}
