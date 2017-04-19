"use strict";

(function () {
    var kotlinJsLocation = process.env.KOTLIN_JS_LOCATION || __dirname + "/../../../dist/js/kotlin.js";
    var Module = require('module');
    var originalRequire = Module.prototype.require;

    Module.prototype.require = function(id) {
        if (id === "kotlin") {
            id = kotlinJsLocation;
        }
        return originalRequire.call(this, id);
    };
})();

var assert = require('assert');
var fs = require('fs');
var path = require('path');
var kotlinJsTestLocation = process.env.KOTLIN_JS_TEST_LOCATION || __dirname + "/../../../dist/js/kotlin-test.js";

var kotlin = require("kotlin");
var kotlinTest = require(kotlinJsTestLocation);
supplyAsserter(kotlinTest);
var requireFromString = require('require-from-string');

var baseDir = "out";
var model = generateModel(baseDir);
exposeModel(model, "./" + baseDir);

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
                        var result = runTest(require(childPath), childPath);
                        assert.equal("OK", result);
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
 * @returns String|{}
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
    }
    else if (stats.isFile()) {
        if (path.endsWith(".node.js")) {
            return path;
        }
        else {
            return void 0;
        }
    }
    else {
        return void 0;
    }
}

function runTest(testRunner, location) {
    var text = "";
    var fs = require('fs');
    var basePath = path.dirname(location);
    return testRunner(function(fileNames, moduleName) {
        text += 'module.exports = function(kotlin) {\n';
        text += "var exports = void 0;";
        for (var i = 0; i < fileNames.length; ++i) {
            text += fs.readFileSync(basePath + "/" + fileNames[i]) + "\n";
        }
        text += 'var resultModule = typeof emulatedModules != "undefined" ? emulatedModules.' + moduleName + ' : null;\n';
        text += "resultModule = resultModule || this." + moduleName + ";\n";
        text += 'return resultModule || ' + moduleName + ';\n';
        text += "};";
        return requireFromString(text).call({ "kotlin-test": kotlinTest }, kotlin);
    });
}

function supplyAsserter(kotlinTest) {
    kotlinTest.kotlin.test.overrideAsserter_wbnzx$(new kotlinTest.kotlin.test.DefaultAsserter());
}