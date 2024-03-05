define("lib", [], function () {
    function bar() {
        return "(" + Array.prototype.join.call(arguments, "") + ")";
    }
    return { bar }
})
