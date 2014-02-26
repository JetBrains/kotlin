(function () {
    var c = 0;

    Kotlin.A = Kotlin.createClassNow(null,
                                     function () {
                                         this.f = function (i) {
                                             if (i === undefined && c === 0) {
                                                 c = 1;
                                             }
                                             if (i === 2 && c === 1) {
                                                 c = 2;
                                             }
                                         }
                                     }
    );
    Kotlin.getResult = function () {
        return c === 2;
    };
})();

