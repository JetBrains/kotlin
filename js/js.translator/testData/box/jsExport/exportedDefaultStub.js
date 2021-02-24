$kotlin_test_internal$.beginModule();

module.exports = function() {
    var api = require("JS_TESTS").api;
    var ping = api.ping;
    var pong = api.pong;
    var transform = api.transform;
    var Ping = api.Ping;
    var Pong = api.Pong;

    return {
        "ping00": ping(),
        "ping01": ping(undefined, 10),
        "ping10": ping("X"),
        "ping11": ping("Z", 5),

        "pong00": pong(),
        "pong01": pong(undefined, 10),
        "pong10": pong("X"),
        "pong11": pong("Z", 5),

        "transform00": transform(),
        "transform11": transform(-5, function(it) { return it * it * it }),

        "Ping_ping00a": new Ping().ping(),
        "Ping_ping00b": new Ping(10).ping(),
        "Ping_ping11": new Ping().ping(-4, function(it) { return it * it * it }),

        "Pong_ping00": new Pong().ping()
    };
};

$kotlin_test_internal$.endModule("lib");
