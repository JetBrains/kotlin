/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import formatters from "./index.mjs";
import { color } from "../html/styles.mjs";
import { Formatter } from "./formatter.mjs";
import { FIELD_IDENTIFIER_COLOR } from "../theme/colors.mjs";
import { element, list, object, span} from "../html/tags.mjs";

export const ENTRY_TYPE = Symbol("entry")

function entryHeader({ value: { key, value } }) {
   const keyPresentation = formatters.get(key)?.header(key) ?? object(key)
    const valuePresentation = formatters.get(value)?.header(value) ?? object(value)
   return span([],  [keyPresentation, " => ", valuePresentation])
}

function entryBody({ value: { key, value } }) {
    return list(
        [],  [
            element([], [span([color(FIELD_IDENTIFIER_COLOR)], "key: "), object(key)]),
            element([], [span([color(FIELD_IDENTIFIER_COLOR)], "value: "), object(value)]),
        ])
}

export default Formatter.create(entryHeader, entryBody)
