package foo

fun box() : Boolean {
    if (#(1, 2, 3)._1 != 1) return false;
    if (#("a", "b")._2 != "b") return false;
    val x = #("1", 2, "3", 4, "5", 6);
    if (x._1 != "1") return false;
    if (x._2 != 2) return false;
    if (x._2 != 2) return false;
    if (x._6 != 6) return false;
    if (x._5 != "5") return false;
    if (x._3 != "3") return false;
    if (#(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)._21 != 1) return false;
    return true;
}