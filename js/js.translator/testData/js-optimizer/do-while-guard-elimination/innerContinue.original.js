function box() {
    var i = 0;
    var counter = 0;
    var sum = 0;
    loop: do {
        guard$: {
            i++;
            if (i < 5) {
                for (var j = 0; j < 10; ++j) {
                    counter += j;
                    if (j == 3 && i == 2) break guard$;
                }
            }
            sum += i;
        }
    } while (i < 10);

    if (sum != 53) return "fail1: " + sum;
    if (counter != 141) return "fail2: " + counter;

    return "OK";
}