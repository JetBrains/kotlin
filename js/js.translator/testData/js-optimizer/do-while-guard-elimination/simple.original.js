function box() {
    var i = 0;
    var sum = 0;
    do {
        guard$: {
            i++;
            if (i == 5) {
                break guard$;
            }
            sum += i;
        }
    } while (i < 10);

    if (sum != 50) return "fail: " + sum;

    return "OK";
}