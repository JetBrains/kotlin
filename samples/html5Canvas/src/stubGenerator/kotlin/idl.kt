package org.jetbrains.kotlin.konan.jsinterop.tool

import org.jetbrains.kotlin.konan.jsinterop.tool.Type.*

// This shall be an output of Web IDL parser.
val all = listOf(
    Interface("Context", 
        Attribute("lineWidth", Integer, hasSetter = true),
        Attribute("fillStyle", idlString, hasSetter = true),
        Attribute("strokeStyle", idlString, hasSetter = true),

        Operation("lineTo", Void, Arg("x", Integer), Arg("y", Integer)),
        Operation("moveTo", Void, Arg("x", Integer), Arg("y", Integer)),
        Operation("beginPath", Void),
        Operation("stroke", Void),
        Operation("fillRect", Void, Arg("x", Integer), Arg("y", Integer), Arg("width", Integer), Arg("height", Integer)),
        Operation("fillText", Void, Arg("test", idlString), Arg("x", Integer),  Arg("y", Integer), Arg("maxWidth", Integer)),
        Operation("fill", Void),
        Operation("closePath", Void)
    ),
    Interface("DOMRect",
        Attribute("left", Integer, hasGetter = true),
        Attribute("right", Integer, hasGetter = true),
        Attribute("top", Integer, hasGetter = true),
        Attribute("bottom", Integer, hasGetter = true)
    ),
    Interface("Canvas",
        Operation("getContext", InterfaceRef("Context"), Arg("context", idlString)),
        Operation("getBoundingClientRect", InterfaceRef("DOMRect"))
    ),
    Interface("Document",
        Operation("getElementById", Object, Arg("id", idlString))
    ),
    Interface("MouseEvent",
        Attribute("clientX", Integer, hasGetter = true),
        Attribute("clientY", Integer, hasGetter = true)
    ),
    Interface("Response",
        Operation("json", Object)
    ),
    Interface("Promise",
        Operation("then", InterfaceRef("Promise"), Arg("lambda", Function))
    ),
    Interface("__Global",
        Attribute("document", InterfaceRef("Document"), hasGetter = true),

        Operation("fetch", InterfaceRef("Promise"), Arg("url", idlString)),
        Operation("setInterval", Void, Arg("lambda", Function), Arg("interval", Integer))
    )
)

   
