//FILE: WithExample.java

import lombok.AccessLevel;
import lombok.With;

public class WithExample {

    //todo replace constructors with annotations when supported
    public WithExample() {

    }

    public WithExample(int age, String name) {

    }

    @With private int age = 10;

    @With private String name;

    static void test() {
        WithExample obj = new WithExample().withAge(16).withName("fooo");
    }
}


//FILE: test.kt

class Test {
    fun run() {
        val obj: WithExample = WithExample().withAge(16).withName("fooo")
    }
}
