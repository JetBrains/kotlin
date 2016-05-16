function box() {
    var i;
    var sum = 0;
    var count = 2;
    while (count-- > 0) {
        i = 1;
        while (i < 10) {
            sum += i;
            i++
        }
    }

    if (sum != 90) return "fail: " + sum;

    return "OK"
}