package b

import a.A

fun foo(response: A) {
    if (response.data != null) {
        doSomethingWithNonNullable(response.data!!)
    }
}

fun doSomethingWithNonNullable(data: Any) {
    print(data)
}