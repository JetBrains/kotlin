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
fun box(): String {
    val x = MyValue("A", "B", "C")
    val result = x.defaultValue + x.privateValue + x.publicValue;
    return if (result == "ABC") "OK" else "Error: $x"
}
