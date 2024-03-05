define("lib", [], function () {
    return {
        O: {
            x: 23,
            foo: function(y) {
                return this.x + y;
            }
        }
    };
})