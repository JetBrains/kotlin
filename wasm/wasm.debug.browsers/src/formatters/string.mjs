/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import { span } from "../html/tags.mjs";
import { color } from "../html/styles.mjs";
import { Formatter } from "./formatter.mjs";
import { STRING_LITERAL_COLOR } from "../theme/colors.mjs";

function string(kotlinString) {
   const kotlinStringValue =  kotlinString.value

   const chars = kotlinStringValue.$_chars.value;

   let result = '"';
   for (let i = 0; i < chars.length; i++) {
       result += String.fromCharCode(chars[i].value);
   }
   result += '"';

   return span(
       [color(STRING_LITERAL_COLOR)],
       [result]
   )
}

export default Formatter.create(string)
