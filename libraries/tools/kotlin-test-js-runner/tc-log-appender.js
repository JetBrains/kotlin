/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

'use strict';

import {formatMessage, TYPED_MESSAGE} from "./src/teamcity-format"

const consoleLog = console.log.bind(console);

function consoleAppender(layout, timezoneOffset) {
    return (loggingEvent) => {
        consoleLog(
            formatMessage(
                TYPED_MESSAGE,
                layout(
                    loggingEvent,
                    timezoneOffset
                ),
                loggingEvent.level.toString()
            )
        );
    };
}

function configure(config, layouts) {
    let layout = layouts.colouredLayout;
    if (config.layout) {
        layout = layouts.layout(config.layout.type, config.layout);
    }
    return consoleAppender(layout, config.timezoneOffset);
}

module.exports.configure = configure;