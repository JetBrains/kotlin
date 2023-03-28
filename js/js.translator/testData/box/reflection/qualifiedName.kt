// KJS_WITH_FULL_RUNTIME

// FILE: abc.kt

package a.b.c


class A {
    class B {
        class C
    }
}

// FILE: abd.kt

package a.b.d

class A {
    class B {
        class D
    }
}

// FILE: main.kt

import kotlin.reflect.KClass

fun getQualifiedName(c: Any) = c::class.qualifiedName

fun box(): String {
    getQualifiedName(a.b.c.A())
    getQualifiedName(a.b.c.A.B())
    getQualifiedName(a.b.c.A.B.C())

    getQualifiedName(a.b.d.A())
    getQualifiedName(a.b.d.A.B())
    getQualifiedName(a.b.d.A.B.D())

    return "OK"
}
