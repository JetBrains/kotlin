interface I<T>

abstract class A<T> : I<T>

@Deprecated("")
class B : A<String>()

class C<T : CharSequence> : A<T>()