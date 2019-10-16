/*
	MIT License http://www.opensource.org/licenses/mit-license.php
	Author Tobias Koppers @sokra
*/
var fs = require("fs");
var path = require("path");
var async = require("async");
var loaderUtils = require("loader-utils");

var separatorRegex = /[/\\]/;

// Matches only the last occurrence of sourceMappingURL
var baseRegex = "\\s*[@#]\\s*sourceMappingURL\\s*=\\s*([^\\s]*)(?![\\S\\s]*sourceMappingURL)",
    // Matches /* ... */ comments
    regex1 = new RegExp("/\\*" + baseRegex + "\\s*\\*/"),
    // Matches // .... comments
    regex2 = new RegExp("//" + baseRegex + "($|\n|\r\n?)"),
    // Matches DataUrls
    regexDataUrl = /data:[^;\n]+(?:;charset=[^;\n]+)?;base64,([a-zA-Z0-9+/]+={0,2})/;

module.exports = function (input, inputMap) {
    this.cacheable && this.cacheable();
    var resolve = this.resolve;
    var addDependency = this.addDependency;
    var emitWarning = this.emitWarning || function () {
    };
    var match = input.match(regex1) || input.match(regex2);
    if (match) {
        var url = match[1];
        var dataUrlMatch = regexDataUrl.exec(url);
        var callback = this.async();
        if (dataUrlMatch) {
            var mapBase64 = dataUrlMatch[1];
            var mapStr = (new Buffer(mapBase64, "base64")).toString();
            var map;
            try {
                map = JSON.parse(mapStr)
            }
            catch (e) {
                emitWarning("Cannot parse inline SourceMap '" + mapBase64.substr(0, 50) + "': " + e);
                return untouched();
            }
            processMap(map, this.context, callback);
        }
        else {
            resolve(this.context, loaderUtils.urlToRequest(url, true), function (err, result) {
                if (err) {
                    emitWarning("Cannot find SourceMap '" + url + "': " + err);
                    return untouched();
                }
                addDependency(result);
                fs.readFile(result, "utf-8", function (err, content) {
                    if (err) {
                        emitWarning("Cannot open SourceMap '" + result + "': " + err);
                        return untouched();
                    }
                    var map;
                    try {
                        map = JSON.parse(content);
                    }
                    catch (e) {
                        emitWarning("Cannot parse SourceMap '" + url + "': " + e);
                        return untouched();
                    }
                    processMap(map, path.dirname(result), callback);
                });
            }.bind(this));

        }
    }
    else {
        var callback = this.callback;
        return untouched();
    }

    function untouched() {
        callback(null, input, inputMap);
    }

    function resize(arr, size, defval) {
        while (arr.length > size) {
            arr.pop();
        }
        while (arr.length < size) {
            arr.push(defval);
        }
    }

    function processMap(map, context, callback) {
        function setResult(map) {
            callback(null, input.replace(match[0], ''), map);
        }

        var sourcesWithoutContent = [];
        map.sourcesContent = map.sourcesContent || [];
        resize(map.sourcesContent, map.sources.length, null);
        map.sourcesContent.forEach(function (sourceContent, i) {
            if (!sourceContent) {
                sourcesWithoutContent.push({source: map.sources[i], index: i})
            }
            else {
                var source = map.sources[i];
                if (separatorRegex.test(source) && !path.isAbsolute(source)) {
                    map.sources[i] = path.resolve(context, source);
                }
            }
        });

        if (sourcesWithoutContent.length == 0) {
            setResult(map)
        }
        else {
            var sourcePrefix = map.sourceRoot ? map.sourceRoot + "/" : "";
            async.map(sourcesWithoutContent, function (item, callback) {
                var source = sourcePrefix + item.source;
                map.sources = map.sources.map(function (s) {
                    return sourcePrefix + s;
                });
                delete map.sourceRoot;
                resolve(context, loaderUtils.urlToRequest(source, true), function (err, result) {
                    if (err) {
                        // TODO(ilgonmic): Not all JS files have sources content, and we don't extract sources from jar,
                        //  so there is no content for these files
                        // emitWarning("Cannot find source file '" + source + "': " + err);
                        return callback(null, null);
                    }
                    addDependency(result);
                    fs.readFile(result, "utf-8", function (err, content) {
                        if (err) {
                            emitWarning("Cannot open source file '" + result + "': " + err);
                            return callback(null, null);
                        }
                        callback(null, {
                            index: item.index,
                            source: result,
                            content: content
                        });
                    });
                });
            }, function (err, info) {
                info.forEach(function (item) {
                    if (item) {
                        map.sources[item.index] = item.source;
                        map.sourcesContent[item.index] = item.content;
                    }
                });
                setResult(map);
            });
        }
    }
};
