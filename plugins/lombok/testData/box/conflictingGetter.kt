// ISSUE: KT-53965

// FILE: SomeClass.java

import lombok.Getter;

@Getter
public class SomeClass {
    private int num;

    public int getNum() {
        return Math.abs(num);
    }

    public SomeClass(int num) {
        this.num = num;
    }
}

// FILE: test.kt

fun foo(value: SomeClass) = value.num

fun box(): String {
    val result = foo(SomeClass(-42))
    if (result != 42) return "FAIL: $result"
    return "OK"
}
