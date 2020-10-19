annotation class FirstAnnotation
annotation class SecondAnnotation
annotation class ThirdAnnotation

class ZeroClass

@FirstAnnotation
class FirstClass

<warning descr="SSR">@FirstAnnotation
@SecondAnnotation
class SecondClass</warning>

<warning descr="SSR">@FirstAnnotation
@SecondAnnotation
@ThirdAnnotation
class ThirdClass</warning>