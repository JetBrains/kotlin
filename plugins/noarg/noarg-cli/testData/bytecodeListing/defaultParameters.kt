annotation class NoArg

@NoArg
annotation class MetaAnno

@NoArg
interface BaseIntf

@NoArg
class Test(a: String = "", i: Int = 2)

class Test2 : BaseIntf {
    constructor(a: String = "", b: Long = 0L) {}
}

@MetaAnno
class Test3(a: String) {
    constructor() : this("") {}
}