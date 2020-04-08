open class C0(val x: Any?) {}

open class C1 protected (val x1: Int = 1, var x2: Float, x3: ((Int) -> Int)?) : C0(x3){
    fun bar() {
        val y1 = x1;
        val y2 = x2;
    }
}
class C2 : C1(1, 2.5, null<caret>) {
    fun foo() {
        var c = C1(2, 3.5, null);
        c = C1(x1 = 2, x2 = 3.5, x3 = null);
    }
}
