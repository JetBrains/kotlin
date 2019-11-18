/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

'use strict';

import {configureBrowsers, fixMochaTimeout} from "./src/DebugConfigurator";

const karma = require('karma');

const cfg = karma.config;

const karmaConfig = cfg.parseConfig(process.argv[2]);

karmaConfig.singleRun = false;

configureBrowsers(karmaConfig);
fixMochaTimeout(karmaConfig);

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