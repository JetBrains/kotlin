interface I<T>

abstract class A<T> : I<T>

annotation class Anno

@Anno
class B : A<String>()

class C<T : CharSequence> : A<T>()