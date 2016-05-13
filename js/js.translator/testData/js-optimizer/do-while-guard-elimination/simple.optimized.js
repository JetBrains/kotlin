function box() {
    var i = 0;
    var sum = 0;
    do {
        i++;
        if (i == 5) {
            continue;
        }
        sum += i;
    } while (i < 10);

    if (sum != 50) return "fail: " + sum;

    return "OK";
}