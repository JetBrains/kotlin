annotation class AllOpen

@AllOpen
sealed class Sealed {
    class C1 : Sealed() {}
    class C2 : Sealed() {}
}

sealed class Sealed2 {
    @AllOpen
    class C1 : Sealed2() {}

    class C2 : Sealed2() {}
}