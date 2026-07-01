// ISSUE: KT-86886
// FILE: ExplicitBuilderWithTypeParametersLessThanRequired.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class ExplicitBuilderWithTypeParametersLessThanRequired {
    private String x;
    public abstract static class ExplicitBuilderWithTypeParametersLessThanRequiredBuilder<T> { }
}

// FILE: ExplicitBuilderWithTypeParametersMoreThanRequired.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class ExplicitBuilderWithTypeParametersMoreThanRequired {
    private String x;
    public abstract static class ExplicitBuilderWithTypeParametersMoreThanRequiredBuilder<T1, T2, T3> { }
}


// FILE: test.kt

fun main() {
    val obj = ExplicitBuilderWithTypeParametersLessThanRequired.builder().build()
    val obj2 = ExplicitBuilderWithTypeParametersMoreThanRequired.builder().build()
}
