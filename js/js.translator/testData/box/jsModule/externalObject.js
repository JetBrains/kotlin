define("lib", [], function() {
    A = {
        x: 23,
        foo: function(y) {
            return this.x + y;
        }
    };

    return A;
});