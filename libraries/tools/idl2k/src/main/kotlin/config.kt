/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
package org.jetbrains.idl2k

val urls = listOf(
        "https://raw.githubusercontent.com/whatwg/html-mirror/master/source" to "org.w3c.dom",
        "https://html.spec.whatwg.org/" to "org.w3c.dom",
        "https://raw.githubusercontent.com/whatwg/dom/master/dom.html" to "org.w3c.dom",
        "https://dvcs.w3.org/hg/editing/raw-file/tip/editing.html" to "org.w3c.dom",
        "https://www.w3.org/TR/animation-timing/" to "org.w3c.dom",
        "https://www.w3.org/TR/geometry-1/" to "org.w3c.dom",
        "https://www.w3.org/TR/cssom-view/" to "org.w3c.dom",
        "https://www.w3.org/TR/uievents/" to "org.w3c.dom.events",
        "https://www.w3.org/TR/pointerevents/" to "org.w3c.dom.pointerevents",

        "https://drafts.csswg.org/cssom/" to "org.w3c.dom.css",
        "https://www.w3.org/TR/css-masking-1/" to "org.w3c.css.masking",

        "https://w3c.github.io/mediacapture-main/" to "org.w3c.dom.mediacapture",
        "https://www.w3.org/TR/DOM-Parsing/" to "org.w3c.dom.parsing",
        "https://w3c.github.io/clipboard-apis" to "org.w3c.dom.clipboard",
        "https://raw.githubusercontent.com/whatwg/url/master/url.html" to "org.w3c.dom.url",

        "https://www.w3.org/TR/SVG2/single-page.html" to "org.w3c.dom.svg",
        "https://www.khronos.org/registry/webgl/specs/latest/1.0/webgl.idl" to "org.khronos.webgl",
        "https://www.khronos.org/registry/typedarray/specs/latest/typedarray.idl" to "org.khronos.webgl",

        "https://raw.githubusercontent.com/whatwg/xhr/master/Overview.src.html" to "org.w3c.xhr",
        "https://raw.githubusercontent.com/whatwg/fetch/master/Overview.src.html" to "org.w3c.fetch",
        "https://raw.githubusercontent.com/w3c/FileAPI/gh-pages/index.html" to "org.w3c.files",

        "https://raw.githubusercontent.com/whatwg/notifications/master/notifications.html" to "org.w3c.notifications",
        "https://raw.githubusercontent.com/whatwg/fullscreen/master/fullscreen.html" to "org.w3c.fullscreen",
        "https://www.w3.org/TR/vibration/" to "org.w3c.vibration",

        "https://www.w3.org/TR/hr-time/" to "org.w3c.performance",
        "https://www.w3.org/TR/2012/REC-navigation-timing-20121217/" to "org.w3c.performance",

        "https://w3c.github.io/ServiceWorker/" to "org.w3c.workers"
)

val relocations = mapOf(
        "Event" to "org.w3c.dom.events",
        "EventTarget" to "org.w3c.dom.events",
        "EventListener" to "org.w3c.dom.events"
)

val commentOutDeclarations = setOf(
        "MouseEvent.screenX: Double", "MouseEvent.screenY: Double",
        "MouseEvent.clientX: Double", "MouseEvent.clientY: Double",
        "MouseEvent.x: Double", "MouseEvent.y: Double",

        "HTMLAllCollection.namedItem",
        "HTMLAllCollection.get",

        "HTMLFormControlsCollection.namedItem",
        "HTMLFormControlsCollection.get",

        "HTMLPropertiesCollection.namedItem",
        "HTMLPropertiesCollection.get",

        "SVGElement.id"
)

val requiredArguments = setOf(
        "DOMPoint.constructor.point",
        "DOMQuad.constructor.rect"
)

val inheritanceExclude = mapOf(
        "SVGAElement" to setOf("HTMLHyperlinkElementUtils")
)

val kotlinBuiltinInterfaces = mapOf(
        "ItemArrayLike" to GenerateClass("ItemArrayLike", "org.w3c.dom", GenerateDefinitionKind.INTERFACE, emptyList(),
                                         memberAttributes = mutableListOf(GenerateAttribute("length", SimpleType("Int", false), null, false, AttributeKind.VAL, false, false, false, false)),
                                         memberFunctions = mutableListOf(GenerateFunction("item", DynamicType, listOf(
                        GenerateAttribute("index", SimpleType("Int", false), null, false, AttributeKind.ARGUMENT, false, false, false, false)
                ), NativeGetterOrSetter.NONE, false, false)),
                                         constants = emptyList(),
                                         generateBuilderFunction = false,
                                         primaryConstructor = null,
                                         secondaryConstructors = emptyList()
                )
)

val eventSpecifierMapper = mapOf<String, String>(
    "onbeforeunload" to "BeforeUnloadEvent",

    "ondrag" to "DragEvent",
    "ondragend" to "DragEvent",
    "ondragenter" to "DragEvent",
    "ondragexit" to "DragEvent",
    "ondragleave" to "DragEvent",
    "ondragover" to "DragEvent",
    "ondragstart" to "DragEvent",
    "ondrop" to "DragEvent",

    "oncopy" to "ClipboardEvent",
    "oncut" to "ClipboardEvent",
    "onpaste" to "ClipboardEvent",


    "onfetch" to "FetchEvent",

    "onblur" to "FocusEvent",
    "onfocus" to "FocusEvent",

    "onhashchange" to "HashChangeEvent",

    "oninput" to "InputEvent",

    "onkeydown" to "KeyboardEvent",
    "onkeypress" to "KeyboardEvent",
    "onkeyup" to "KeyboardEvent",

    "onmessage" to "MessageEvent",

    "onclick" to "MouseEvent",
    "oncontextmenu" to "MouseEvent",
    "ondblclick" to "MouseEvent",
    "onmousedown" to "MouseEvent",
    "onmouseenter" to "MouseEvent",
    "onmouseleave" to "MouseEvent",
    "onmousemove" to "MouseEvent",
    "onmouseout" to "MouseEvent",
    "onmouseover" to "MouseEvent",
    "onmouseup" to "MouseEvent",

    "onnotificationclick" to "NotificationEvent",
    "onnotificationclose" to "NotificationEvent",

    "onpagehide" to "PageTransitionEvent",
    "onpageshow" to "PageTransitionEvent",

    "ongotpointercapture" to "PointerEvent",
    "onlostpointercapture" to "PointerEvent",
    "onpointercancel" to "PointerEvent",
    "onpointerdown" to "PointerEvent",
    "onpointerenter" to "PointerEvent",
    "onpointerleave" to "PointerEvent",
    "onpointermove" to "PointerEvent",
    "onpointerout" to "PointerEvent",
    "onpointerover" to "PointerEvent",
    "onpointerup" to "PointerEvent",

    "onpopstate" to "PopStateEvent",

    "onloadstart" to "ProgressEvent",
    "onprogress" to "ProgressEvent",

    "onunhandledrejection" to "PromiseRejectionEvent",

    "onstorage" to "StorageEvent",

    "onwheel" to "WheelEvent"
)


data class EventMapKey(val name: String, val context: String)

val eventSpecifierMapperWithContext = mapOf<EventMapKey, String>(
    EventMapKey("onaddtrack", "MediaStream") to "MediaStreamTrackEvent",
    EventMapKey("onremovetrack", "MediaStream") to "MediaStreamTrackEvent",

    EventMapKey("onaddtrack", "AudioTrackList") to "TrackEvent",
    EventMapKey("onaddtrack", "TextTrackList") to "TrackEvent",
    EventMapKey("onaddtrack", "VideoTrackList") to "TrackEvent",
    EventMapKey("onremovetrack", "AudioTrackList") to "TrackEvent",
    EventMapKey("onremovetrack", "TextTrackList") to "TrackEvent",
    EventMapKey("onremovetrack", "VideoTrackList") to "TrackEvent"
)
