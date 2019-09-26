/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

const util = require('util');
const resolve = require('url').resolve;

const escapeMessage = function (message) {
    if (message === null || message === undefined) {
        return ''
    }

    return message.toString()
        .replace(/\|/g, '||')
        .replace(/'/g, "|'")
        .replace(/\n/g, '|n')
        .replace(/\r/g, '|r')
        .replace(/\u0085/g, '|x')
        .replace(/\u2028/g, '|l')
        .replace(/\u2029/g, '|p')
        .replace(/\[/g, '|[')
        .replace(/]/g, '|]')
};

const formatMessage = function () {
    const args = Array.prototype.slice.call(arguments);

    for (let i = args.length - 1; i > 0; i--) {
        args[i] = escapeMessage(args[i])
    }

    return util.format.apply(null, args) + '\n'
};

// This ErrorFormatter is copied from standard karma's,
//  but without warning in case of failed original location finding
function createFormatError(config, emitter) {
    const basePath = config.basePath;
    const urlRoot = config.urlRoot === '/' ? '' : (config.urlRoot || '');
    let lastServedFiles = [];

    emitter.on('file_list_modified', (files) => {
        lastServedFiles = files.served
    });
    const URL_REGEXP = new RegExp('(?:https?:\\/\\/' +
                                  config.hostname + '(?:\\:' + config.port + ')?' + ')?\\/?' +
                                  urlRoot + '\\/?' +
                                  '(base/|absolute)' + // prefix, including slash for base/ to create relative paths.
                                  '((?:[A-z]\\:)?[^\\?\\s\\:]*)' + // path
                                  '(\\?\\w*)?' + // sha
                                  '(\\:(\\d+))?' + // line
                                  '(\\:(\\d+))?' + // column
                                  '', 'g');

    const SourceMapConsumer = require('source-map').SourceMapConsumer;

    const cache = new WeakMap();

    function getSourceMapConsumer(sourceMap) {
        if (!cache.has(sourceMap)) {
            cache.set(sourceMap, new SourceMapConsumer(sourceMap))
        }
        return cache.get(sourceMap)
    }

    const PathUtils = require('karma/lib/utils/path-utils.js');

    return function (input) {
        let msg = input.replace(URL_REGEXP, function (_, prefix, path, __, ___, line, ____, column) {
            const normalizedPath = prefix === 'base/' ? `${basePath}/${path}` : path;
            const file = lastServedFiles.find((file) => file.path === normalizedPath);

            if (file && file.sourceMap && line) {
                line = +line;
                column = +column;
                const bias = column ? SourceMapConsumer.GREATEST_LOWER_BOUND : SourceMapConsumer.LEAST_UPPER_BOUND;

                try {
                    const original = getSourceMapConsumer(file.sourceMap).originalPositionFor({line, column: (column || 0), bias});

                    return `${PathUtils.formatPathMapping(resolve(path, original.source), original.line,
                                                          original.column)} <- ${PathUtils.formatPathMapping(path, line, column)}`
                }
                catch (e) {
                    // do nothing
                }
            }

            return PathUtils.formatPathMapping(path, line, column) || prefix
        });

        return msg + '\n'
    };
}

// This reporter extends karma-teamcity-reporter
//  It is necessary, because karma-teamcity-reporter can't write browser's log
//  And additionally it overrides flushLogs, because flushLogs adds redundant spaces after some messages
const KarmaKotlinReporter = function (baseReporterDecorator, config, emitter) {
    const teamcityReporter = require("karma-teamcity-reporter")["reporter:teamcity"][1];
    teamcityReporter.call(this, baseReporterDecorator);

    const formatError = createFormatError(config, emitter);

    this.TEST_STD_OUT = "##teamcity[testStdOut name='%s' out='%s' flowId='']";

    const tcOnBrowserStart = this.onBrowserStart;
    this.onBrowserStart = function (browser) {
        tcOnBrowserStart.call(this, browser);
        this.browserResults[browser.id].consoleCollector = [];
    };

    this.onBrowserLog = (browser, log, type) => {
        const browserResult = this.browserResults[browser.id];
        if (browserResult) {
            browserResult.consoleCollector.push(`[${type}] ${log}\n`)
        }
    };

    const tcSpecSuccess = this.specSuccess;
    this.specSuccess = function (browser, result) {
        tcSpecSuccess.call(this, browser, result);

        const log = this.getLog(browser, result);
        const testName = result.description;

        const endMessage = log.pop();
        this.browserResults[browser.id].consoleCollector.forEach(item => {
            log.push(
                formatMessage(this.TEST_STD_OUT, testName, item)
            )
        });
        log.push(endMessage);

        this.browserResults[browser.id].consoleCollector = []
    };

    this.specFailure = function (browser, result) {
        const log = this.getLog(browser, result);
        const testName = result.description;

        log.push(formatMessage(this.TEST_START, testName));
        this.browserResults[browser.id].consoleCollector.forEach(item => {
            log.push(
                formatMessage(this.TEST_STD_OUT, testName, item)
            )
        });

        log.push(formatMessage(this.TEST_FAILED, testName,
                               result.log
                                   .map(log => formatError(log))
                                   .join('\n\n')
        ));
        log.push(formatMessage(this.TEST_END, testName, result.time));

        this.browserResults[browser.id].consoleCollector = []
    };

    this.flushLogs = function (browserResult) {
        while (browserResult.log.length > 0) {
            let line = browserResult.log.shift();
            line = line.replace("flowId=''", "flowId='" + browserResult.flowId + "'");

            this.write(line);
        }
    }
};

KarmaKotlinReporter.$inject = ['baseReporterDecorator', 'config', 'emitter'];

module.exports = {
    'reporter:karma-kotlin-reporter': ['type', KarmaKotlinReporter]
};
