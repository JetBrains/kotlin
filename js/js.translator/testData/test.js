"use strict";

var assert = require('assert');
var fs = require('fs');
var kotlin = require(process.env.KOTLIN_JS_LOCATION);
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