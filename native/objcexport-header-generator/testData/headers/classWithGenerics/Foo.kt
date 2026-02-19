open class A<T1, T2>

interface I1<T>
interface I2<T>

class Foo<T> : I1<T>, I2<String>, A<T, String>()