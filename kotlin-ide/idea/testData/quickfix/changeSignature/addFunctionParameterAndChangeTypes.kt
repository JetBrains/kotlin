// "Change the signature of function 'foo'" "true"
// DISABLE-ERRORS

fun foo(x: Int, i: Double) {
    foo();
    foo(1);
    foo(1, 2.5);
    foo(1.5, 4, <caret>5, 6);
    foo(2, 3, sdsd);
}