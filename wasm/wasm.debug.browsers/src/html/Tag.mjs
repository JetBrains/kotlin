/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import { Style } from "./Style.mjs";

export class Tag {
    static create(name, styles, children) {
        const [classNames, pureStyles] = styles.reduce(
            (x, style) => style instanceof Style ? [x[0].concat(style), x[1]]: [x[0], x[1].concat(style)]
            , [[], []]
        )
        return [name, { style: pureStyles.join(";"), "class": classNames.join(" ") }].concat(children)
    }

    static object(object) {
        return ["object", { object }]
    }
}
