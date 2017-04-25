// EXPECTED_REACHABLE_NODES: 502
package foo

import kotlin.reflect.KProperty

interface WithName {
    var name: String
}

class GetPropertyName() {
    operator fun getValue(withName: WithName, property: KProperty<*>): String {
        return withName.name + ":" + property.name;
    }
    operator fun setValue(withName: WithName, property: KProperty<*>, value: String) {
        withName.name = value + ":" + property.name
    }
}

class A : WithName {
    override var name = "propertyName"
    val d = GetPropertyName()

    val a by d
    var OK by d
}

fun box(): String {
    val a = A()
    if (a.a != "propertyName:a") return "a.a != 'propertyName:a', it: " + a.a
    if (a.OK != "propertyName:OK") return "a.OK != 'propertyName:aOK', it: " + a.OK
    a.OK = "property"

    if (a.a != "property:OK:a") return "a.a != 'property:OK:a', it: " + a.a

    return "OK"
}
