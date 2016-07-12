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
/*Asserter*/

var JsTests = (function () {
    var reporter = jsTestReporter;
    var failedTest = {};

    var assert = function (isTrue, message) {
        if (!isTrue) {
            fail(message);
        }
    };

    var fail = function (message) {
        reporter.reportError(message);
        throw failedTest;
    };

    var init = function () {
        init = function() {};
        kotlin.modules.JS_TESTS.kotlin.test.init();
    };

    var test = function (testName, testFun) {
        init();

        reporter.testStart(testName);
        try {
            testFun();
        }
        catch (fail) {
            if (fail != failedTest) {
                reporter.reportError("Unexpected exception " + fail + "\n" + fail.stack);
            }
            reporter.testFail(testName);
            return;
        }
        reporter.testSuccess(testName);
    };
    return {
        test: test,
        assert: assert,
        fail: fail
    }
})();
