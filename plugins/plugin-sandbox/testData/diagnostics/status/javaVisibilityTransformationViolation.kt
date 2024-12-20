// ISSUE: KT-74098
// FILE: main.kt
fun function() {
    JavaClass.<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Nested<!>()
}

// FILE: JavaClass.java
public class JavaClass<T extends JavaClass.Neste> extends JavaClass.Nested<T> {
    public static class Nested<T extends JavaClass.Neste> {
    }
}
