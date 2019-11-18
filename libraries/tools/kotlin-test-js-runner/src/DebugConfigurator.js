/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

const REMOTE_DEBUGGING_PORT = '--remote-debugging-port';

export function configureBrowsers(config) {
    let newBrowsers = config.browsers;
    if (!Array.isArray(newBrowsers)) {
        newBrowsers = [];
    }

    let browser = newBrowsers.find(browserName => isDebuggableBrowser(browserName, config));

    if (!browser) {
        console.warn(
            'Unable to find debuggable browser: Only Google Chrome with 9222 remote debugging port supported\n',
            'Fallback on Chrome Headless'
        );
        browser = 'ChromeHeadless'
    }

    config.browsers = [browser];
}

function isDebuggableBrowser(browserName, config) {
    if ([
        'ChromeHeadless',
        'ChromeCanaryHeadless',
        'ChromiumHeadless'
    ].includes(browserName)) {
        return true
    }

    const customLaunchers = config.customLaunchers;
    if (!customLaunchers) {
        return false;
    }

    let customLauncher = customLaunchers[browserName];
    if (!customLauncher) {
        return false;
    }

    const flags = customLauncher.flags;
    if (!Array.isArray(flags)) {
        return false;
    }

    const prefix = REMOTE_DEBUGGING_PORT + '=';
    const value = flags.find(flag => typeof flag === 'string' && flag.indexOf(prefix) === 0);
    if (value == null) {
        return false;
    }

    const port = parseInt(value.substring(prefix.length), 10);
    if (isNaN(port) || port !== 9222) {
        console.error(`Debugger expect 9222 port, but ${port} found`);
        return false;
    }

    return true;
}

export function fixMochaTimeout(config) {
    let client = config.client;
    if (typeof client === 'undefined') {
        config.client = client = {};
    }
    if (client === Object(client)) {
        let mocha = client.mocha;
        if (typeof mocha === 'undefined') {
            client.mocha = mocha = {};
        }
        if (mocha === Object(mocha)) {
            mocha.timeout = 0;
        }
        else {
            console.error('config.client.mocha is not an object');
            process.exit(1);
        }
    }
    else {
        console.error('config.client is not an object');
        process.exit(1);
    }
}