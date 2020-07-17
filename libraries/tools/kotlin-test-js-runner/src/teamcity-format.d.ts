/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

export const TEST_IGNORED: string
export const SUITE_START: string
export const SUITE_END: string
export const SUITE_END_NO_DURATION: string
export const TEST_START: string
export const TEST_FAILED: string
export const TEST_FAILED_COMPARISON: string
export const TEST_END: string
export const TEST_END_NO_DURATION: string
export const BLOCK_OPENED: string
export const BLOCK_CLOSED: string
export const TYPED_MESSAGE: string

export function tcEscape(str: string): string

export function formatMessage(type: string, ...str: string[]): string