package defaultImpls;

interface AllAbstract {
    val c: Int
    fun <T> f(t: T): T

    val g: Int

    var g2: String
}

interface NonAbstractFun {
    fun f() {

    }
}

interface NonAbstractFunWithExpressionBody {
    fun f() = 3
}

interface NonAbstractProperty {
    val c: Int get() = 3
}

interface NonAbstractPropertyWithBody {
    val c: Int get() {
        return 3
    }
}