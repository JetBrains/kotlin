/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

'use strict';

const karma = require('karma');

const cfg = karma.config;

const karmaConfig = cfg.parseConfig(process.argv[2]);

karmaConfig.singleRun = false;

configureBrowsers(karmaConfig);

const Server = karma.Server;
const server = new Server(karmaConfig, function (exitCode) {
    console.log('Karma has exited with ' + exitCode);
});

server.on('browsers_ready', function () {
    // It is unreliable decision, but we need some delay for debugger attaching
    setTimeout(function () {
        karma.runner.run(karmaConfig, function (exitCode) {
            console.log('Runner has exited with ' + exitCode);
            karma.stopper.stop(karmaConfig, function (exitCode) {
                console.log('Stopper has exited with ' + exitCode);
            })
        })
    }, 1000)
});

server.start();

function configureBrowsers(config) {
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

const REMOTE_DEBUGGING_PORT = '--remote-debugging-port';

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
        console.warn(`Debugger expect 9222 port, but ${port} found`);
        return false;
    }

    return true;
}