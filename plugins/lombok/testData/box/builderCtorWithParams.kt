// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-83325

// FILE: TestJava.java
import lombok.Builder;

@Builder
class TestJava {
    int a;
    final String b;
}

// FILE: TestJavaUsage.java
public class TestJavaUsage {
    public static void main(String[] args) {
        TestJava test = new TestJava(1, "str");          //OK
    }
}

// FILE: test.kt
fun box(): String {
    TestJava(<!TOO_MANY_ARGUMENTS!>1<!>, <!TOO_MANY_ARGUMENTS!>"str"<!>)
    return "OK"
}
