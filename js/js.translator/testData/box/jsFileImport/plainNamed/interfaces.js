define("lib", [], function () {
    var bar = {
        ping() {
            return "ping"
        }
    };

    var baz = {
        pong() {
            return 194
        }
    };

    return { bar, baz }
})
