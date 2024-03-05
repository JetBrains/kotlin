define("lib", [], function () {
    return function() {
        return "(" + Array.prototype.join.call(arguments, "") + ")";
    };
})
