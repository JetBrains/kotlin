/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.native.interop.gen.wasm.idl

// There are no WebIDL descriptions of Math,
// so in any case this one will be a part of the project.
// Although, may be in the form of our own WebIDL source.

val idlMath = listOf(
    Interface("Math",
        Attribute("E",      idlDouble,  readOnly = true, isStatic = true),
        Attribute("LN2",    idlDouble,  readOnly = true, isStatic = true),
        Attribute("LN10",   idlDouble,  readOnly = true, isStatic = true),
        Attribute("LOG2E",  idlDouble,  readOnly = true, isStatic = true),
        Attribute("LOG10E", idlDouble,  readOnly = true, isStatic = true),
        Attribute("PI",     idlDouble,  readOnly = true, isStatic = true),
        Attribute("SQRT1_2", idlDouble, readOnly = true, isStatic = true),
        Attribute("SQRT2",  idlDouble,  readOnly = true, isStatic = true),

        Operation("abs",    idlDouble,  true,  Arg("x", idlDouble)),
        Operation("acos",   idlDouble,  true,  Arg("x", idlDouble)),
        Operation("acosh",  idlDouble,  true,  Arg("x", idlDouble)),
        Operation("asin",   idlDouble,  true,  Arg("x", idlDouble)),
        Operation("asinh",  idlDouble,  true,  Arg("x", idlDouble)),
        Operation("atan",   idlDouble,  true,  Arg("x", idlDouble)),
        Operation("atanh",  idlDouble,  true,  Arg("x", idlDouble)),
        Operation("atan2",  idlDouble,  true,  Arg("y", idlDouble), Arg("x", idlDouble)),
        Operation("cbrt",   idlDouble,  true,  Arg("x", idlDouble)),
        Operation("ceil",   idlDouble,  true,  Arg("x", idlDouble)),
        Operation("clz32",  idlDouble,  true,  Arg("x", idlDouble)),
        Operation("cos",    idlDouble,  true,  Arg("x", idlDouble)),
        Operation("cosh",   idlDouble,  true,  Arg("x", idlDouble)),
        Operation("exp",    idlDouble,  true,  Arg("x", idlDouble)),
        Operation("expm1",  idlDouble,  true,  Arg("x", idlDouble)),
        Operation("floor",  idlDouble,  true,  Arg("x", idlDouble)),
        Operation("fround", idlDouble,  true,  Arg("x", idlDouble)),
        //Operation("imul(x,    y),
        Operation("log",    idlDouble,  true,  Arg("x", idlDouble)),
        Operation("log1p",  idlDouble,  true,  Arg("x", idlDouble)),
        Operation("log10",  idlDouble,  true,  Arg("x", idlDouble)),
        Operation("log2",   idlDouble,  true,  Arg("x", idlDouble)),
        Operation("pow",    idlDouble,  true,  Arg("x", idlDouble), Arg("y", idlDouble)),
        Operation("random", idlDouble,  true),
        Operation("round",  idlDouble,  true,  Arg("x", idlDouble)),
        Operation("sign",   idlDouble,  true,  Arg("x", idlDouble)),
        Operation("sin",    idlDouble,  true,  Arg("x", idlDouble)),
        Operation("sinh",   idlDouble,  true,  Arg("x", idlDouble)),
        Operation("sqrt",   idlDouble,  true,  Arg("x", idlDouble)),
        Operation("tan",    idlDouble,  true,  Arg("x", idlDouble)),
        Operation("tanh",   idlDouble,  true,  Arg("x", idlDouble)),
        Operation("trunc",  idlDouble,  true,  Arg("x", idlDouble)),

        // Actually these functions have vararg parameter.
        // But their kotlin analogs have only 2 parameters so we don't need to support varargs here.
        Operation("hypot",  idlDouble,  true,  Arg("x", idlDouble), Arg("y", idlDouble)),
        Operation("max",    idlDouble,  true,  Arg("x", idlDouble), Arg("y", idlDouble)),
        Operation("min",    idlDouble,  true,  Arg("x", idlDouble), Arg("y", idlDouble))
    )
)
