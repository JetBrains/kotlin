/**
 * From mocha-teamcity-reporter
 * The MIT License
 * Copyright (c) 2016 Jamie Sherriff
 */
export const TEST_IGNORED = `##teamcity[testIgnored name='%s' message='%s' flowId='%s']`;
export const SUITE_START = `##teamcity[testSuiteStarted name='%s' flowId='%s']`;
export const SUITE_END = `##teamcity[testSuiteFinished name='%s' duration='%s' flowId='%s']`;
export const SUITE_END_NO_DURATION = `##teamcity[testSuiteFinished name='%s' flowId='%s']`;
export const TEST_START = `##teamcity[testStarted name='%s' captureStandardOutput='true' flowId='%s']`;
export const TEST_FAILED = `##teamcity[testFailed name='%s' message='%s' details='%s' captureStandardOutput='true' flowId='%s']`;
export const TEST_FAILED_COMPARISON = `##teamcity[testFailed type='comparisonFailure' name='%s' message='%s' \
details='%s' captureStandardOutput='true' actual='%s' expected='%s' flowId='%s']`;
export const TEST_END = `##teamcity[testFinished name='%s' duration='%s' flowId='%s']`;
export const TEST_END_NO_DURATION = `##teamcity[testFinished name='%s' flowId='%s']`;
export const BLOCK_OPENED = `##teamcity[blockOpened name='%s' flowId='%s']`
export const BLOCK_CLOSED = `##teamcity[blockClosed name='%s' flowId='%s']`

export const TYPED_MESSAGE = `##teamcity[message text='%s' type='%s']`

/**
 * from teamcity-service-messages
 * Copyright (c) 2013 Aaron Forsander
 *
 * Escape string for TeamCity output.
 * @see https://confluence.jetbrains.com/display/TCD65/Build+Script+Interaction+with+TeamCity#BuildScriptInteractionwithTeamCity-servMsgsServiceMessages
 */

const format = require('format-util');

export function tcEscape(str) {
    if (!str) {
        return '';
    }

    return str
        .toString()
        .replace(/\x1B.*?m/g, '') // eslint-disable-line no-control-regex
        .replace(/\|/g, '||')
        .replace(/\n/g, '|n')
        .replace(/\r/g, '|r')
        .replace(/\[/g, '|[')
        .replace(/\]/g, '|]')
        .replace(/\u0085/g, '|x') // next line
        .replace(/\u2028/g, '|l') // line separator
        .replace(/\u2029/g, '|p') // paragraph separator
        .replace(/'/g, '|\'');
}

export function formatMessage() {
    let formattedArguments = [];
    const args = Array.prototype.slice.call(arguments, 0);
    // Format all arguments for TC display (it escapes using the pipe char).
    let tcMessage = args.shift();
    args.forEach((param) => {
        formattedArguments.push(tcEscape(param));
    });
    formattedArguments.unshift(tcMessage);
    return format(formattedArguments);
}