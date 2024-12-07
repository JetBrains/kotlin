/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import { color } from "../html/styles.mjs";
import { Formatter } from "./formatter.mjs";
import { getFqnNameFrom } from "../type-checkers/index.mjs";
import { element, list, object, span } from "../html/tags.mjs";
import { CLASS_IDENTIFIER_COLOR, FIELD_IDENTIFIER_COLOR} from "../theme/colors.mjs";

const IGNORED_FIELDS = new Set([
    "$_hashCode",
    "$itable",
    "$typeInfo",
    "$vtable"
])

function classHeader(kotlinClass) {
    return span([color(CLASS_IDENTIFIER_COLOR)], [getFqnNameFrom(kotlinClass.type)])
}

function classBody(kotlinClass) {
    const fields = []
    for (const key in kotlinClass.value) {
       if (IGNORED_FIELDS.has(key)) continue;
       fields.push(
           element(
               [],
               [
                   span([color(FIELD_IDENTIFIER_COLOR)], [key.slice(1), ": "]),
                   object(kotlinClass.value[key])
               ])
       )
    }
    return list([], fields);
}

export default Formatter.create(classHeader, classBody)
