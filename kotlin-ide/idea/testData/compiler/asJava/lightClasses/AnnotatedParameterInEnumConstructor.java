public enum AnnotatedParameterInEnumConstructor {
    A;

    private AnnotatedParameterInEnumConstructor(@test.Anno(x = "a") java.lang.String a, @test.Anno(x = "b") java.lang.String b) { /* compiled code */ }
}