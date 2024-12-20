// ISSUE: KT-74097
// FILE: main.kt
fun function() {
    JavaClass.<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Nested<!>()
}

// FILE: JavaClass.java
public class JavaClass<T extends JavaClass.Nested> extends JavaClass.Nested<T> {
    public static class Nested<T extends JavaClass.Nested> {
    }
}
