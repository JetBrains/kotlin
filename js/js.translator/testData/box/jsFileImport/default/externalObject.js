define("lib", [], function () {
    return {
        x: 23,
        foo: function(y) {
            return this.x + y;
        }
    };
})