/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import { span } from "../html/tags.mjs";
import { color } from "../html/styles.mjs";
import { Formatter } from "./formatter.mjs";
import { KEYWORD_COLOR } from "../theme/colors.mjs";

export function _null() {
    return span(
        [color(KEYWORD_COLOR)],
        ["null"]
    )
}

export default Formatter.create(_null)
