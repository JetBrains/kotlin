package foo

public sealed class <caret>SealedClass {
    public class Impl1 : SealedClass() {}
}

public class Impl2 : SealedClass() {}