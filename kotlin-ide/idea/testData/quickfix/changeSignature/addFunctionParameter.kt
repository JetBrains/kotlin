// "Add parameter to function 'foo'" "true"
// DISABLE-ERRORS

fun foo(x: Int) {
    foo();
    foo(1);
    foo(1, 4<caret>);
    foo(2, 3, sdsd);
}