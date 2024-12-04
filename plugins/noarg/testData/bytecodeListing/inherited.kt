annotation class NoArg

@NoArg
annotation class MyAnno

@MyAnno
interface Base

@MyAnno
class Test(a: String) : Base