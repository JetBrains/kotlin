/**
 * From mocha-teamcity-reporter
 * The MIT License
 * Copyright (c) 2016 Jamie Sherriff
 */

/**
 * Teamcity doc reference https://confluence.jetbrains.com/display/TCD10/Build+Script+Interaction+with+TeamCity
 *
 * Module dependencies.
 */
'use strict';

import {
    formatMessage,
    SUITE_END,
    SUITE_END_NO_DURATION,
    SUITE_START,
    TEST_END,
    TEST_END_NO_DURATION,
    TEST_FAILED,
    TEST_FAILED_COMPARISON,
    TEST_IGNORED,
    TEST_START
} from "./src/teamcity-format";

const processPID = process.pid.toString();

let Base, log, logError;

Base = require('mocha').reporters.Base;
log = console.log;
logError = console.error;

function isNil(value) {
    return value == null; 	// eslint-disable-line
}

/**
 * Initialize a new `Teamcity` reporter.
 *
 * @param {Runner} runner
 * @param {options} options
 * @api public
 */

function Teamcity(runner, options) {
    options = options || {};
    const reporterOptions = options.reporterOptions || {};
    let flowId, useStdError, recordHookFailures, actualVsExpected;
    (reporterOptions.flowId) ? flowId = reporterOptions.flowId : flowId = process.env['MOCHA_TEAMCITY_FLOWID'] || processPID;
    (reporterOptions.useStdError) ? useStdError = reporterOptions.useStdError : useStdError = process.env['USE_STD_ERROR'];
    (reporterOptions.recordHookFailures) ? recordHookFailures = reporterOptions.recordHookFailures : recordHookFailures =
        process.env['RECORD_HOOK_FAILURES'];
    (reporterOptions.actualVsExpected) ? actualVsExpected = reporterOptions.actualVsExpected : actualVsExpected =
        process.env['ACTUAL_VS_EXPECTED'];
    (useStdError) ? useStdError = (useStdError.toLowerCase() === 'true') : useStdError = false;
    (recordHookFailures) ? recordHookFailures = (recordHookFailures.toLowerCase() === 'true') : recordHookFailures = false;
    actualVsExpected ? actualVsExpected = (actualVsExpected.toLowerCase() === 'true') : actualVsExpected = false;
    Base.call(this, runner);
    let stats = this.stats;
    const topLevelSuite = reporterOptions.topLevelSuite || process.env['MOCHA_TEAMCITY_TOP_LEVEL_SUITE'];

    runner.on('suite', function (suite) {
        if (suite.root) {
            if (topLevelSuite) {
                log(formatMessage(SUITE_START, topLevelSuite, flowId));
            }
            return;
        }
        suite.startDate = new Date();
        log(formatMessage(SUITE_START, suite.title, flowId));
    });

    runner.on('test', function (test) {
        log(formatMessage(TEST_START, test.title, flowId));
    });

    runner.on('fail', function (test, err) {
        if (actualVsExpected && (err.actual && err.expected)) {
            if (useStdError) {
                logError(formatMessage(TEST_FAILED_COMPARISON, test.title, err.message, err.stack, err.actual,
                                       err.expected, flowId));
            }
            else {
                log(formatMessage(TEST_FAILED_COMPARISON, test.title, err.message, err.stack, err.actual,
                                  err.expected, flowId));
            }
        }
        else {
            if (useStdError) {
                logError(formatMessage(TEST_FAILED, test.title, err.message, err.stack, flowId));
            }
            else {
                log(formatMessage(TEST_FAILED, test.title, err.message, err.stack, flowId));
            }
        }
    });

    runner.on('pending', function (test) {
        log(formatMessage(TEST_IGNORED, test.title, test.title, flowId));
    });

    runner.on('test end', function (test) {
        // This is necessary not to emit `test end` event on skipped tests
        if (test.isPending()) return

        if (isNil(test.duration)) {
            log(formatMessage(TEST_END_NO_DURATION, test.title, flowId));
        }
        else {
            log(formatMessage(TEST_END, test.title, test.duration.toString(), flowId));
        }
    });

    runner.on('hook', function (test) {
        if (recordHookFailures) {
            log(formatMessage(TEST_START, test.title, flowId));
        }
    });

    runner.on('suite end', function (suite) {
        if (suite.root) return;
        log(formatMessage(SUITE_END, suite.title, new Date() - suite.startDate, flowId));
    });

    runner.on('end', function () {
        let duration;
        (typeof stats === 'undefined') ? duration = null : duration = stats.duration;
        if (topLevelSuite) {
            isNil(duration) ? log(formatMessage(SUITE_END_NO_DURATION, topLevelSuite, flowId)) : log(
                formatMessage(SUITE_END, topLevelSuite, duration, flowId));
        }
    });
}


/**
 * Expose `Teamcity`.
 */

exports = module.exports = Teamcity;