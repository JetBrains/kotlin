/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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