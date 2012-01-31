function test() {
    var a = Kotlin.arrayFromFun(3, function () {
        return 3
    });
    return (a[0] == 3) && (a[2] == 3) && (a[1] == 3);
}