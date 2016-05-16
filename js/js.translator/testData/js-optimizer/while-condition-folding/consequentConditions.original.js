function box() {
    var i = 1;
    var sum = 0;
    while (true) {
        if (i >= 10) {
            break;
        }
        if (sum > 30) {
            break;
        }
        sum += i;
        i++
    }

    if (sum != 36) return "fail: " + sum;

    return "OK"
}