"use strict";

var assert = require('assert');
var fs = require('fs');
var kotlinJsLocation = process.env.KOTLIN_JS_LOCATION;
if (!kotlinJsLocation) {
    kotlinJsLocation = "../../../dist/js/kotlin.js";
}
var kotlin = require(kotlinJsLocation);
supplyAsserter(kotlin);
var requireFromString = require('require-from-string');

var model = generateModel("out");
exposeModel(model, "./out");

function exposeModel(model, path) {
    for (var property in model) {
        if (!model.hasOwnProperty(property)) {
            continue;
        }

        var childPath = path + "/" + property;
        var item = model[property];
        describe(property, function(childPath, item) {
            return function() {
                if (typeof item === "string") {
                    it("", function () {
                        var result = require(childPath);
                        assert.equal("OK", result(kotlin, requireFromString));
                    });
                }
                else if (typeof item === "object") {
                    exposeModel(item, childPath);
                }
            }
        }(childPath, item));
    }
}

/**
 * @param path String
 * @returns String|{*}
 */
function generateModel(path) {
    var stats = fs.statSync(path);
    if (stats.isDirectory()) {
        var result = {};
        var files = fs.readdirSync(path);
        var empty = true;
        for (var i = 0; i < files.length; ++i) {
            var child = files[i];
            var childModel = generateModel(path + "/" + child);
            if (childModel !== void 0) {
                result[child] = childModel;
                empty = false;
            }
        }
        return !empty ? result : void 0;
    } else if (stats.isFile()) {
        if (path.endsWith(".node.js")) {
            return path;
        } else {
            return void 0;
        }
    } else {
        return void 0;
    }
}

function supplyAsserter(kotlin) {
    var asserterContainer = kotlin.defineRootPackage(null, {
        asserter : kotlin.createClass(
            function () {
                return [kotlin.kotlin.test.Asserter];
            },
            function () {},
            {
                assertTrue_tup0fe$: function (lazyMessage, actual) {
                    kotlin.kotlin.test.assertTrue_8kj6y5$(actual, lazyMessage());
                },
                assertTrue_ivxn3r$: function (message, actual) {
                    if (!actual) {
                        this.failWithMessage(message);
                    }
                },
                fail_61zpoe$: function (message) {
                    this.failWithMessage(message);
                },
                failWithMessage: function (message) {
                    if (message == null) {
                        throw new Kotlin.AssertionError();
                    } else {
                        throw new Kotlin.AssertionError(message);
                    }
                }
            }
        )
    });
    kotlin.kotlin.test.asserter = new asserterContainer.asserter();
}