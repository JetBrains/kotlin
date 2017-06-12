// EXPECTED_REACHABLE_NODES: 525
interface BK {
    fun x(): Int = 50
}

interface K : BK {
    override fun x(): Int = super.x() * 2
}

open class M() {
    open fun x(): Int = 10

    open var y = 500
}

open class N() : M(), K {

    override fun x(): Int = 20

    override var y = 200

    inner open class C() : K {
        fun test1() = x()
        fun test2() = super<M>@N.x()
        fun test3() = super<K>@N.x()
        fun test4() = super<K>.x()
        fun test5() = y
        fun test6(): Int {
            super<M>@N.y += 200
            return super<M>@N.y
        }
    }
}

fun box(): String {
    if (N().C().test1() != 100) return "test1 fail";
    if (N().C().test2() != 10) return "test2 fail";
    if (N().C().test3() != 100) return "test3 fail";
    if (N().C().test4() != 100) return "test4 fail";
    if (N().C().test5() != 200) return "test5 fail";
    if (N().C().test6() != 700) return "test6 fail";
    return "OK";
}