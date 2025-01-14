// FIR_IDENTICAL
// ISSUE: KT-73027, KT-73029

// FILE: Klass.java

@lombok.Builder(builderMethodName = ) // Specified argument name without a value
public class Klass {
    String str;
}

// FILE: Klass2.java

@lombok.Builder(buildMethodName = "myBuild", ) // Neither name nor value is specified
public class Klass2 {
    int integer;
}

// FILE: test.kt

fun test() {
    val klassBuilder: Klass.KlassBuilder = Klass.build()
    val klass2Builder: Klass2.Klass2Builder = Klass2.myBuild()
}
