function test() {
    var a = new Kotlin.Array(3, function () {
        return 3
    });
    a.set(0, "1")
    a.set(2, "4")
    return (a.get(0) == "1") && (a.get(2) == "4") && (a.get(1) == 3);
}