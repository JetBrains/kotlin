(function () {
    QUnit.init();
    QUnit.config.blocking = true;
    QUnit.config.autorun = true;
    QUnit.config.updateRate = 0;
    QUnit.results = []
    QUnit.log = function (log) {
        var outcome = log.result ? "PASS" : "FAIL";
        QUnit.results.push(outcome + log.message)
    };
})();


function runQUnitSuite() {
    QUnit.begin();
    QUnit.start();

    /*
    var answer = ""
    var results = QUnit.results;
    for (var i = 0, size = results.length; i < size; i++) {
        answer += results[i];
        answer += "\n";
    }
    return answer
    */
    return QUnit.results;
}