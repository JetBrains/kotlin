interface I<T>

abstract class A<T> : I<T>

annotation class Anno

@Anno
class B : A<String>()

class C<T : CharSequence> : A<T>()

interface I2<X>
open class B2<X>
class A2<T : CharSequence> : B2<T>(), I2<T>

interface I3<T>
class A3<T> : I3<List<T>>