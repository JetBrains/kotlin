// EXPECTED_REACHABLE_NODES: 1281
package foo

class Foo{
    data class Id(val uuid: Int)
}
class Bar{
    data class Id(val uuid: Int)
}

class E1 {
    enum class Id { A }
}

class E2 {
    enum class Id { A }
}

class O1 {
    object Id
}

class O2 {
    object Id
}

class Service{
    operator fun get(id: Foo.Id) = "Foo getter"
    operator fun get(id: Bar.Id) = "Bar getter"

    operator fun get(id: E1.Id) = "E1 getter"
    operator fun get(id: E2.Id) = "E2 getter"

    operator fun get(id: O1.Id) = "O1 getter"
    operator fun get(id: O2.Id) = "O2 getter"
}

fun box(): String {
    var service = Service()
    if (service[Bar.Id(12)] != "Bar getter") return "Fail with /**/Bar overload"
    if (service[Foo.Id(6)] != "Foo getter") return "Fail with Foo overload"
    if (service[E1.Id.A] != "E1 getter") return "Fail with E1 overload"
    if (service[E2.Id.A] != "E2 getter") return "Fail with E2 overload"
    if (service[O1.Id] != "O1 getter") return "Fail with O1 overload"
    if (service[O2.Id] != "O2 getter") return "Fail with O2 overload"
    return "OK"
}