/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import {color, paddingLeft, rem} from "../html/styles.mjs";
import { Formatter } from "./formatter.mjs";
import { FIELD_IDENTIFIER_COLOR } from "../theme/colors.mjs";
import { element, list, object, span } from "../html/tags.mjs";

const MAX_ELEMENTS_PER_FOLD = 100
const POWER_OF_TEN_FOR_MAX_ELEMENTS = Math.log10(MAX_ELEMENTS_PER_FOLD)
export const FOLDED_TYPE = Symbol("FOLDED_TYPE")

function foldedListHeader({ value: { start, end }}) {
    return span([], [`[${start} … ${end - 1}]`])
}

export function foldedListBody({ value: { start, end, elementAt }}) {
    let elements;
    const length = end - start;
    const foldsDepth = Math.ceil(Math.ceil(Math.log10(length) / POWER_OF_TEN_FOR_MAX_ELEMENTS)) - 1;
    if (foldsDepth <= 0) {
        elements = new Array(length);
        for (let i = 0; i < length; i++) {
            const index = start + i;
            const keyPresentation = span([color(FIELD_IDENTIFIER_COLOR)], [index, ": "]);
            const valuePresentation = object(elementAt(index));
            elements[i] = element( [], [keyPresentation, valuePresentation]);
        }
    } else {
        const elementsPerFold = MAX_ELEMENTS_PER_FOLD ** foldsDepth
        const foldsNumber = Math.ceil(length / elementsPerFold)
        elements = new Array(foldsNumber);
        for (let i = 0; i < foldsNumber; i++) {
            const from = elementsPerFold * i;
            const to = Math.min(from + elementsPerFold, end)
            elements[i] = element([], [object({ type: FOLDED_TYPE, value: { start: from, end: to, elementAt } })])
        }
    }

    return list([paddingLeft(rem(2))], elements);
}

export default Formatter.create(foldedListHeader, foldedListBody)
