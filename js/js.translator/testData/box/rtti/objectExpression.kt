// EXPECTED_REACHABLE_NODES: 555
package foo

interface A
interface B
open class C
open class D

fun box(): String {

    if ((object {} as Any) is A) return "object {} is A"
    if ((object {} as Any) is C) return "object {} is C"

    if ((object : A {} as Any) !is A) return "object : A {} !is A"
    if ((object : A {} as Any) is B) return "object : A {} is B"
    if ((object : A {} as Any) is C) return "object : A {} is C"

    if ((object : C() {} as Any) is A) return "object : C() {} is A"
    if ((object : C() {} as Any) !is C) return "object : C() {} !is C"
    if ((object : C() {} as Any) is D) return "object : C() {} is D"

    if ((object : B, D() {} as Any) is A) return "object : B, D() {} is A"
    if ((object : B, D() {} as Any) !is B) return "object : B, D() {} !is B"
    if ((object : B, D() {} as Any) is C) return "object : B, D() {} is C"
    if ((object : B, D() {} as Any) !is D) return "object : B, D() {} !is D"

    if ((object : D(), B {} as Any) is A) return "object : D(), B {} is A"
    if ((object : D(), B {} as Any) !is B) return "object : D(), B {} !is B"
    if ((object : D(), B {} as Any) is C) return "object : D(), B {} is C"
    if ((object : D(), B {} as Any) !is D) return "object : D(), B {} !is D"

    return "OK"
}
