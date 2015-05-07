/*
 * Generated file
 * DO NOT EDIT
 * 
 * See libraries/tools/idl2k for details
 */

package org.w3c.dom.parsing

import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.css.*
import org.w3c.dom.events.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.workers.*
import org.w3c.xhr.*

native public open class DOMParser {
    fun parseFromString(str: String, type: dynamic): Document = noImpl
}

native public open class XMLSerializer {
    fun serializeToString(root: Node): String = noImpl
}

native public marker trait UnionElementOrHTMLCollection {
}

native public marker trait UnionElementOrRadioNodeList {
}

native public marker trait UnionHTMLOptGroupElementOrHTMLOptionElement {
}

native public marker trait UnionAudioTrackOrTextTrackOrVideoTrack {
}

native public marker trait UnionElementOrMouseEvent {
}

native public marker trait UnionElementOrProcessingInstruction {
}

native public marker trait UnionMessagePortOrServiceWorker {
}

native public marker trait UnionClientOrMessagePortOrServiceWorker {
}

