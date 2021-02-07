public interface Trait {
    @org.jetbrains.annotations.NotNull
    java.lang.String notNull(@org.jetbrains.annotations.NotNull java.lang.String p);

    @org.jetbrains.annotations.Nullable
    java.lang.String nullable(@org.jetbrains.annotations.Nullable java.lang.String p);

    @org.jetbrains.annotations.NotNull
    java.lang.String notNullWithNN();

    @org.jetbrains.annotations.Nullable
    @org.jetbrains.annotations.NotNull
    java.lang.String notNullWithN();

    @org.jetbrains.annotations.Nullable
    java.lang.String nullableWithN();

    @org.jetbrains.annotations.NotNull
    @org.jetbrains.annotations.Nullable
    java.lang.String nullableWithNN();

    @org.jetbrains.annotations.Nullable
    java.lang.String getNullableVal();

    @org.jetbrains.annotations.Nullable
    java.lang.String getNullableVar();

    void setNullableVar(@org.jetbrains.annotations.Nullable java.lang.String p);

    @org.jetbrains.annotations.NotNull
    java.lang.String getNotNullVal();

    @org.jetbrains.annotations.NotNull
    java.lang.String getNotNullVar();

    void setNotNullVar(@org.jetbrains.annotations.NotNull java.lang.String p);
}