open class C1 protected (x3: ((Int) -> Int)?, var _x2: Float, val _x1: Int = 1) {
    fun bar() {
        val y1 = _x1;
        val y2 = _x2;
    }
}
class C2 : C1(null, 2.5, 1) {
    fun foo() {
        var c = C1(null, 3.5, 2);
        c = C1(x3 = null, _x2 = 3.5, _x1 = 2);
    }
}
