// ISSUE: KT-51092
// FILE: MyValue.java
import lombok.Value;

@Value
public class MyValue {
    String defaultValue;
    private String privateValue;
    public String publicValue;
}

// FILE: main.kt
import kotlin.test.assertEquals

fun box(): String {
    val x = MyValue("A", "B", "C")
    val result = x.defaultValue + x.privateValue + x.publicValue;
    assertEquals("ABC", result)

    return "OK"
}
