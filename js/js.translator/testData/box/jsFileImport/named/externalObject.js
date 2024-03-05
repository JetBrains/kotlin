define("lib", [], function () {
    const O = {
        x: 23,
        foo: function(y) {
            return this.x + y;
        }
    };

    return { O }
})