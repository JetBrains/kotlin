function test(x) {
    $outer: {
        if (x > 10) {
            break $outer;
        }
    }
    return "OK";
}

function box() {
    return test(23);
}