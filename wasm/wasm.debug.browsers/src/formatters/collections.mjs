/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import { color } from "../html/styles.mjs";
import { Formatter } from "./formatter.mjs";
import { getFqnNameFrom } from "../type-checkers/index.mjs";
import { foldedListBody } from "./folded-list.mjs";
import { element, object, span } from "../html/tags.mjs";
import { ENTRY_TYPE } from "./entry.mjs";
import { CLASS_IDENTIFIER_COLOR, FIELD_IDENTIFIER_COLOR } from "../theme/colors.mjs";
import {int} from "./int.mjs";

export function listsHeader({ count, lengthFieldName }) {
    return kotlinClass => {
        const length = count(kotlinClass);
        return span(
            [color(CLASS_IDENTIFIER_COLOR)],
            [
                span([], [
                    getFqnNameFrom(kotlinClass.type),
                    " {",
                    span([], [`${lengthFieldName} = ${length}`]),
                    "}"
                ])
            ]
        )
    }
}

export function listsBody({ elementAt, count, lengthFieldName }) {
    return kotlinClass => {
        return foldedListBody({
            value: {
                start: 0,
                end: count(kotlinClass),
                elementAt: index => elementAt(kotlinClass, index),
            }
        }).concat([
            element(
                [],
                [
                    span([color(FIELD_IDENTIFIER_COLOR)], [`${lengthFieldName}: `]),
                    int({ value: count(kotlinClass) })
                ]
            )
        ])
    }
}

const arrayShape = {
    lengthFieldName: "length",
    count: kotlinClass => kotlinClass.value.$storage.value.length,
    elementAt: (kotlinClass, i) => kotlinClass.value.$storage.value[i]
}
export const array = Formatter.create(
    listsHeader(arrayShape),
    listsBody(arrayShape)
)

const arrayListShape = {
    lengthFieldName: "size",
    count: kotlinClass => arrayShape.count(kotlinClass.value.$backing),
    elementAt: (kotlinClass, i) => arrayShape.elementAt(kotlinClass.value.$backing, i)
}
export const arrayList = Formatter.create(
    listsHeader(arrayListShape),
    listsBody(arrayListShape)
)

const hashSetShape = {
    lengthFieldName: "size",
    count: kotlinClass => hashMapShape.count(kotlinClass.value.$backing),
    elementAt: (kotlinClass, i) => arrayShape.elementAt(kotlinClass.value.$backing.value.$keysArray, i)
}
export const hashSet = Formatter.create(
    listsHeader(hashSetShape),
    listsBody(hashSetShape)
)

const hashMapShape = {
    lengthFieldName: "size",
    count: kotlinClass => kotlinClass.value.$_size.value,
    elementAt: (kotlinClass, i) => ({
        type: ENTRY_TYPE,
        value: {
            key: arrayShape.elementAt(kotlinClass.value.$keysArray, i),
            value: arrayShape.elementAt(kotlinClass.value.$valuesArray, i),
        }
    })
}
export const hashMap = Formatter.create(
    listsHeader(hashMapShape),
    listsBody(hashMapShape)
)
