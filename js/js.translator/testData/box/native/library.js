(function () {
    var c = 0;

    kotlin.A = function() {
        this.f = function (i) {
            if (i === undefined && c === 0) {
                c = 1;
            }
            if (i === 2 && c === 1) {
                c = 2;
            }
        }
    };
    kotlin.getResult = function () {
        return c === 2;
    };
})();

