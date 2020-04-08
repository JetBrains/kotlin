// DISABLE-ERRORS
fun foo() {
    <selection>a > b[n] && (a < foo(x.bar(n + 2)) || a == n) && b[n - 1] != foo(a + 2)</selection>
    a > b[n] && a < foo(x.bar(n + 2)) || (a == n) && (b[n - 1] != foo(a + 2))
    a > b[n] && (a < foo(x.bar(n + 2)) || (a == n)) && (b[n - 1] != foo(a + 2))
}