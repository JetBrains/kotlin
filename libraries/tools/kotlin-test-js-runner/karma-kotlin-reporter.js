const util = require('util');
const resolve = require('path').resolve;

/**
 * From karma
 * The MIT License
 * Copyright (C) 2011-2019 Google, Inc.
 */
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

/**
 * From karma-teamcity-reporter.
 * The MIT License
 * Copyright (C) 2011-2013 Vojta Jína and contributors
 */
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

const hashString = function (s) {
    let hash = 0
    let i
    let chr
    let len

    if (s === 0) return hash
    for (i = 0, len = s.length; i < len; i++) {
        chr = s.charCodeAt(i)
        hash = ((hash << 5) - hash) + chr
        hash |= 0
    }
    return hash
}

const formatMessage = function () {
    const args = Array.prototype.slice.call(arguments);

    for (let i = args.length - 1; i > 0; i--) {
        args[i] = escapeMessage(args[i])
    }

    return util.format.apply(null, args) + '\n'
};

// This reporter extends karma-teamcity-reporter
//  It is necessary, because karma-teamcity-reporter can't write browser's log
//  And additionally it overrides flushLogs, because flushLogs adds redundant spaces after some messages
const KarmaKotlinReporter = function (baseReporterDecorator, config, emitter) {
    baseReporterDecorator(this)
    const self = this

    const formatError = createFormatError(config, emitter)

    this.TEST_IGNORED = "##teamcity[testIgnored name='%s' flowId='']"
    this.SUITE_START = "##teamcity[testSuiteStarted name='%s' flowId='']"
    this.SUITE_END = "##teamcity[testSuiteFinished name='%s' flowId='']"
    this.TEST_START = "##teamcity[testStarted name='%s' flowId='']"
    this.TEST_FAILED = "##teamcity[testFailed name='%s' message='FAILED' details='%s' flowId='']"
    this.TEST_END = "##teamcity[testFinished name='%s' duration='%s' flowId='']"
    this.BLOCK_OPENED = "##teamcity[blockOpened name='%s' flowId='']"
    this.BLOCK_CLOSED = "##teamcity[blockClosed name='%s' flowId='']"

    const END_KOTLIN_TEST = "'--END_KOTLIN_TEST--"

    const reporter = this
    const initializeBrowser = function (browser) {
        reporter.browserResults[browser.id] = {
            name: browser.name,
            log: [],
            consoleCollector: [],
            consoleResultCollector: [],
            lastSuite: null,
            flowId: 'karmaTC' + hashString(browser.name + ((new Date()).getTime())) + browser.id
        }
    }

    this.onRunStart = function (browsers) {
        this.write(formatMessage(this.BLOCK_OPENED, 'JavaScript Unit Tests'))

        this.browserResults = {}
        // Support Karma 0.10 (TODO: remove)
        browsers.forEach(initializeBrowser)
    }

    this.onBrowserStart = function (browser) {
        initializeBrowser(browser)
    }

    const concatenateFqn = function (result) {
        return `${result.suite.join(".")}.${result.description}`
    };

    this.onBrowserLog = (browser, log, type) => {
        const browserResult = this.browserResults[browser.id];

        if (log.startsWith(END_KOTLIN_TEST)) {
            const result = JSON.parse(log.substring(END_KOTLIN_TEST.length, log.length - 1));
            browserResult.consoleResultCollector[concatenateFqn(result)] = browserResult.consoleCollector;
            browserResult.consoleCollector = [];
            return
        }

        if (browserResult) {
            browserResult.consoleCollector.push(log.slice(1, -1))
        }
    };

    this.specSuccess = function (browser, result) {
        const log = this.getLog(browser, result)
        const testName = result.description

        log.push(formatMessage(this.TEST_START, testName))
        this.browserResults[browser.id].consoleResultCollector[concatenateFqn(result)].forEach(item => {
            log.push(item)
        });

        log.push(formatMessage(this.TEST_END, testName, result.time))
    }

    this.specFailure = function (browser, result) {
        const log = this.getLog(browser, result)
        const testName = result.description

        log.push(formatMessage(this.TEST_START, testName))

        this.browserResults[browser.id].consoleResultCollector[concatenateFqn(result)].forEach(item => {
            log.push(item)
        });

        log.push(formatMessage(this.TEST_FAILED, testName,
                               result.log
                                   .map(log => formatError(log))
                                   .join('\n\n')
        ));

        log.push(formatMessage(this.TEST_END, testName, result.time))
    }

    this.specSkipped = function (browser, result) {
        const log = this.getLog(browser, result)
        const testName = result.description

        log.push(formatMessage(this.TEST_IGNORED, testName))
    }

    this.onRunComplete = function () {
        Object.keys(this.browserResults).forEach(function (browserId) {
            const browserResult = self.browserResults[browserId]
            const log = browserResult.log
            if (browserResult.lastSuite) {
                log.push(formatMessage(self.SUITE_END, browserResult.lastSuite))
            }

            self.flushLogs(browserResult)
        })
        self.write(formatMessage(self.BLOCK_CLOSED, 'JavaScript Unit Tests'))
    }

    this.getLog = function (browser, result) {
        const browserResult = this.browserResults[browser.id]
        let suiteName = browser.name
        const moduleName = result.suite.join(' ')

        if (moduleName) {
            suiteName = moduleName.concat('.', suiteName)
        }

        const log = browserResult.log
        if (browserResult.lastSuite !== suiteName) {
            if (browserResult.lastSuite) {
                log.push(formatMessage(this.SUITE_END, browserResult.lastSuite))
            }
            this.flushLogs(browserResult)
            browserResult.lastSuite = suiteName
            log.push(formatMessage(this.SUITE_START, suiteName))
        }
        return log
    }

    this.flushLogs = function (browserResult) {
        while (browserResult.log.length > 0) {
            let line = browserResult.log.shift();
            line = line.replace("flowId=''", "flowId='" + browserResult.flowId + "'");

            this.write(line);
        }
    }
}

KarmaKotlinReporter.$inject = ['baseReporterDecorator', 'config', 'emitter'];

module.exports = {
    'reporter:karma-kotlin-reporter': ['type', KarmaKotlinReporter]
};
