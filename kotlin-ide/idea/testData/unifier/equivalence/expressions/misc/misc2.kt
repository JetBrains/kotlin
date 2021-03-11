// DISABLE-ERRORS
fun foo() {
    <selection>(a.foo((n + 2)*(m - 1))[k[i]] is MyClass?) || (b.foo(n - 2)[i + 1] !is YourClass)</selection>
    a.foo((n + 2*m - 1))[k[i]] is MyClass? || b.foo[n - 2](i + 1) !is YourClass
    a.foo((n + 2)*(m - 1))[k[i]] is MyClass? || b.foo(n - 2)[i + 1] !is YourClass
}