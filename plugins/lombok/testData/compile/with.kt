//FILE: WithExample.java

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
public class WithExample {

    @Getter @With private int age = 10;

    @Getter @With private String name;

    public static WithExample test() {
        return new WithExample().withAge(16).withName("fooo");
    }
}


//FILE: test.kt

class Test {

    fun run() {
        val obj: WithExample = WithExample().withAge(16).withName("fooo")
        assertEquals(obj.getName(), "fooo")
        assertEquals(obj.age, 16)
    }
}
