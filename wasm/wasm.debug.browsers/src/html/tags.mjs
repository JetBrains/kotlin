/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import { Tag } from "./Tag.mjs";

export function span(styles, children) {
    return Tag.create("span", styles, children);
}

export function list(styles, children) {
    return Tag.create("div", styles, children);
}

export function element(styles, children) {
    return Tag.create("li", styles, children);
}

export function object(value) {
    return Tag.object(value);
}