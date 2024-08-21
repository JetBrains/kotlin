/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import { Style } from "./Style.mjs";
import { DARK_THEME, LIGHT_THEME } from "../theme/colors.mjs";

export function color(colorRef) {
    const theme = window.matchMedia("(prefers-color-scheme: dark)").matches
        ? DARK_THEME
        : LIGHT_THEME;
   return Style.create("color", theme[colorRef.description]);
}

export function paddingLeft(value) {
   return Style.create("padding-left", value);
}

export function rem(value) {
  return `${value}rem`
}