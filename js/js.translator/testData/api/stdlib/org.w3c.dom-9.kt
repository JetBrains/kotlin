
public abstract external class TextMetrics {
    /*primary*/ public constructor TextMetrics()
    public open val actualBoundingBoxAscent: kotlin.Double
        public open fun <get-actualBoundingBoxAscent>(): kotlin.Double
    public open val actualBoundingBoxDescent: kotlin.Double
        public open fun <get-actualBoundingBoxDescent>(): kotlin.Double
    public open val actualBoundingBoxLeft: kotlin.Double
        public open fun <get-actualBoundingBoxLeft>(): kotlin.Double
    public open val actualBoundingBoxRight: kotlin.Double
        public open fun <get-actualBoundingBoxRight>(): kotlin.Double
    public open val alphabeticBaseline: kotlin.Double
        public open fun <get-alphabeticBaseline>(): kotlin.Double
    public open val emHeightAscent: kotlin.Double
        public open fun <get-emHeightAscent>(): kotlin.Double
    public open val emHeightDescent: kotlin.Double
        public open fun <get-emHeightDescent>(): kotlin.Double
    public open val fontBoundingBoxAscent: kotlin.Double
        public open fun <get-fontBoundingBoxAscent>(): kotlin.Double
    public open val fontBoundingBoxDescent: kotlin.Double
        public open fun <get-fontBoundingBoxDescent>(): kotlin.Double
    public open val hangingBaseline: kotlin.Double
        public open fun <get-hangingBaseline>(): kotlin.Double
    public open val ideographicBaseline: kotlin.Double
        public open fun <get-ideographicBaseline>(): kotlin.Double
    public open val width: kotlin.Double
        public open fun <get-width>(): kotlin.Double
}

public abstract external class TextTrack : org.w3c.dom.events.EventTarget, org.w3c.dom.UnionAudioTrackOrTextTrackOrVideoTrack {
    /*primary*/ public constructor TextTrack()
    public open val activeCues: org.w3c.dom.TextTrackCueList?
        public open fun <get-activeCues>(): org.w3c.dom.TextTrackCueList?
    public open val cues: org.w3c.dom.TextTrackCueList?
        public open fun <get-cues>(): org.w3c.dom.TextTrackCueList?
    public open val id: kotlin.String
        public open fun <get-id>(): kotlin.String
    public open val inBandMetadataTrackDispatchType: kotlin.String
        public open fun <get-inBandMetadataTrackDispatchType>(): kotlin.String
    public open val kind: org.w3c.dom.TextTrackKind
        public open fun <get-kind>(): org.w3c.dom.TextTrackKind
    public open val label: kotlin.String
        public open fun <get-label>(): kotlin.String
    public open val language: kotlin.String
        public open fun <get-language>(): kotlin.String
    public open var mode: org.w3c.dom.TextTrackMode
        public open fun <get-mode>(): org.w3c.dom.TextTrackMode
        public open fun <set-mode>(/*0*/ <set-?>: org.w3c.dom.TextTrackMode): kotlin.Unit
    public open var oncuechange: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-oncuechange>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-oncuechange>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open val sourceBuffer: org.w3c.dom.mediasource.SourceBuffer?
        public open fun <get-sourceBuffer>(): org.w3c.dom.mediasource.SourceBuffer?
    public final fun addCue(/*0*/ cue: org.w3c.dom.TextTrackCue): kotlin.Unit
    public final fun removeCue(/*0*/ cue: org.w3c.dom.TextTrackCue): kotlin.Unit
}

public abstract external class TextTrackCue : org.w3c.dom.events.EventTarget {
    /*primary*/ public constructor TextTrackCue()
    public open var endTime: kotlin.Double
        public open fun <get-endTime>(): kotlin.Double
        public open fun <set-endTime>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open var id: kotlin.String
        public open fun <get-id>(): kotlin.String
        public open fun <set-id>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var onenter: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onenter>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onenter>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onexit: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onexit>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onexit>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var pauseOnExit: kotlin.Boolean
        public open fun <get-pauseOnExit>(): kotlin.Boolean
        public open fun <set-pauseOnExit>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open var startTime: kotlin.Double
        public open fun <get-startTime>(): kotlin.Double
        public open fun <set-startTime>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open val track: org.w3c.dom.TextTrack?
        public open fun <get-track>(): org.w3c.dom.TextTrack?
}

public abstract external class TextTrackCueList {
    /*primary*/ public constructor TextTrackCueList()
    public open val length: kotlin.Int
        public open fun <get-length>(): kotlin.Int
    public final fun getCueById(/*0*/ id: kotlin.String): org.w3c.dom.TextTrackCue?
}

public external interface TextTrackKind {

    public companion object Companion {
    }
}

public abstract external class TextTrackList : org.w3c.dom.events.EventTarget {
    /*primary*/ public constructor TextTrackList()
    public open val length: kotlin.Int
        public open fun <get-length>(): kotlin.Int
    public open var onaddtrack: ((org.w3c.dom.TrackEvent) -> dynamic)?
        public open fun <get-onaddtrack>(): ((org.w3c.dom.TrackEvent) -> dynamic)?
        public open fun <set-onaddtrack>(/*0*/ <set-?>: ((org.w3c.dom.TrackEvent) -> dynamic)?): kotlin.Unit
    public open var onchange: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onchange>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onchange>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onremovetrack: ((org.w3c.dom.TrackEvent) -> dynamic)?
        public open fun <get-onremovetrack>(): ((org.w3c.dom.TrackEvent) -> dynamic)?
        public open fun <set-onremovetrack>(/*0*/ <set-?>: ((org.w3c.dom.TrackEvent) -> dynamic)?): kotlin.Unit
    public final fun getTrackById(/*0*/ id: kotlin.String): org.w3c.dom.TextTrack?
}

public external interface TextTrackMode {

    public companion object Companion {
    }
}

public abstract external class TimeRanges {
    /*primary*/ public constructor TimeRanges()
    public open val length: kotlin.Int
        public open fun <get-length>(): kotlin.Int
    public final fun end(/*0*/ index: kotlin.Int): kotlin.Double
    public final fun start(/*0*/ index: kotlin.Int): kotlin.Double
}

public abstract external class Touch {
    /*primary*/ public constructor Touch()
    public open val clientX: kotlin.Int
        public open fun <get-clientX>(): kotlin.Int
    public open val clientY: kotlin.Int
        public open fun <get-clientY>(): kotlin.Int
    public open val identifier: kotlin.Int
        public open fun <get-identifier>(): kotlin.Int
    public open val pageX: kotlin.Int
        public open fun <get-pageX>(): kotlin.Int
    public open val pageY: kotlin.Int
        public open fun <get-pageY>(): kotlin.Int
    public open val region: kotlin.String?
        public open fun <get-region>(): kotlin.String?
    public open val screenX: kotlin.Int
        public open fun <get-screenX>(): kotlin.Int
    public open val screenY: kotlin.Int
        public open fun <get-screenY>(): kotlin.Int
    public open val target: org.w3c.dom.events.EventTarget
        public open fun <get-target>(): org.w3c.dom.events.EventTarget
}

public open external class TouchEvent : org.w3c.dom.events.UIEvent {
    /*primary*/ public constructor TouchEvent()
    public open val altKey: kotlin.Boolean
        public open fun <get-altKey>(): kotlin.Boolean
    public open val changedTouches: org.w3c.dom.TouchList
        public open fun <get-changedTouches>(): org.w3c.dom.TouchList
    public open val ctrlKey: kotlin.Boolean
        public open fun <get-ctrlKey>(): kotlin.Boolean
    public open val metaKey: kotlin.Boolean
        public open fun <get-metaKey>(): kotlin.Boolean
    public open val shiftKey: kotlin.Boolean
        public open fun <get-shiftKey>(): kotlin.Boolean
    public open val targetTouches: org.w3c.dom.TouchList
        public open fun <get-targetTouches>(): org.w3c.dom.TouchList
    public open val touches: org.w3c.dom.TouchList
        public open fun <get-touches>(): org.w3c.dom.TouchList

    public companion object Companion {
        public final val AT_TARGET: kotlin.Short
            public final fun <get-AT_TARGET>(): kotlin.Short
        public final val BUBBLING_PHASE: kotlin.Short
            public final fun <get-BUBBLING_PHASE>(): kotlin.Short
        public final val CAPTURING_PHASE: kotlin.Short
            public final fun <get-CAPTURING_PHASE>(): kotlin.Short
        public final val NONE: kotlin.Short
            public final fun <get-NONE>(): kotlin.Short
    }
}

public abstract external class TouchList : org.w3c.dom.ItemArrayLike<org.w3c.dom.Touch> {
    /*primary*/ public constructor TouchList()
    public open override /*1*/ fun item(/*0*/ index: kotlin.Int): org.w3c.dom.Touch?
}

public open external class TrackEvent : org.w3c.dom.events.Event {
    /*primary*/ public constructor TrackEvent(/*0*/ type: kotlin.String, /*1*/ eventInitDict: org.w3c.dom.TrackEventInit = ...)
    public open val track: org.w3c.dom.UnionAudioTrackOrTextTrackOrVideoTrack?
        public open fun <get-track>(): org.w3c.dom.UnionAudioTrackOrTextTrackOrVideoTrack?

    public companion object Companion {
        public final val AT_TARGET: kotlin.Short
            public final fun <get-AT_TARGET>(): kotlin.Short
        public final val BUBBLING_PHASE: kotlin.Short
            public final fun <get-BUBBLING_PHASE>(): kotlin.Short
        public final val CAPTURING_PHASE: kotlin.Short
            public final fun <get-CAPTURING_PHASE>(): kotlin.Short
        public final val NONE: kotlin.Short
            public final fun <get-NONE>(): kotlin.Short
    }
}

public external interface TrackEventInit : org.w3c.dom.EventInit {
    public open var track: org.w3c.dom.UnionAudioTrackOrTextTrackOrVideoTrack?
        public open fun <get-track>(): org.w3c.dom.UnionAudioTrackOrTextTrackOrVideoTrack?
        public open fun <set-track>(/*0*/ value: org.w3c.dom.UnionAudioTrackOrTextTrackOrVideoTrack?): kotlin.Unit
}

public abstract external class TreeWalker {
    /*primary*/ public constructor TreeWalker()
    public open var currentNode: org.w3c.dom.Node
        public open fun <get-currentNode>(): org.w3c.dom.Node
        public open fun <set-currentNode>(/*0*/ <set-?>: org.w3c.dom.Node): kotlin.Unit
    public open val filter: org.w3c.dom.NodeFilter?
        public open fun <get-filter>(): org.w3c.dom.NodeFilter?
    public open val root: org.w3c.dom.Node
        public open fun <get-root>(): org.w3c.dom.Node
    public open val whatToShow: kotlin.Int
        public open fun <get-whatToShow>(): kotlin.Int
    public final fun firstChild(): org.w3c.dom.Node?
    public final fun lastChild(): org.w3c.dom.Node?
    public final fun nextNode(): org.w3c.dom.Node?
    public final fun nextSibling(): org.w3c.dom.Node?
    public final fun parentNode(): org.w3c.dom.Node?
    public final fun previousNode(): org.w3c.dom.Node?
    public final fun previousSibling(): org.w3c.dom.Node?
}

public external interface UnionAudioTrackOrTextTrackOrVideoTrack {
}

public external interface UnionElementOrHTMLCollection {
}

public external interface UnionElementOrMouseEvent {
}

public external interface UnionElementOrRadioNodeList {
}

public external interface UnionHTMLOptGroupElementOrHTMLOptionElement {
}

public external interface UnionMessagePortOrWindowProxy {
}

public abstract external class ValidityState {
    /*primary*/ public constructor ValidityState()
    public open val badInput: kotlin.Boolean
        public open fun <get-badInput>(): kotlin.Boolean
    public open val customError: kotlin.Boolean
        public open fun <get-customError>(): kotlin.Boolean
    public open val patternMismatch: kotlin.Boolean
        public open fun <get-patternMismatch>(): kotlin.Boolean
    public open val rangeOverflow: kotlin.Boolean
        public open fun <get-rangeOverflow>(): kotlin.Boolean
    public open val rangeUnderflow: kotlin.Boolean
        public open fun <get-rangeUnderflow>(): kotlin.Boolean
    public open val stepMismatch: kotlin.Boolean
        public open fun <get-stepMismatch>(): kotlin.Boolean
    public open val tooLong: kotlin.Boolean
        public open fun <get-tooLong>(): kotlin.Boolean
    public open val tooShort: kotlin.Boolean
        public open fun <get-tooShort>(): kotlin.Boolean
    public open val typeMismatch: kotlin.Boolean
        public open fun <get-typeMismatch>(): kotlin.Boolean
    public open val valid: kotlin.Boolean
        public open fun <get-valid>(): kotlin.Boolean
    public open val valueMissing: kotlin.Boolean
        public open fun <get-valueMissing>(): kotlin.Boolean
}

public abstract external class VideoTrack : org.w3c.dom.UnionAudioTrackOrTextTrackOrVideoTrack {
    /*primary*/ public constructor VideoTrack()
    public open val id: kotlin.String
        public open fun <get-id>(): kotlin.String
    public open val kind: kotlin.String
        public open fun <get-kind>(): kotlin.String
    public open val label: kotlin.String
        public open fun <get-label>(): kotlin.String
    public open val language: kotlin.String
        public open fun <get-language>(): kotlin.String
    public open var selected: kotlin.Boolean
        public open fun <get-selected>(): kotlin.Boolean
        public open fun <set-selected>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open val sourceBuffer: org.w3c.dom.mediasource.SourceBuffer?
        public open fun <get-sourceBuffer>(): org.w3c.dom.mediasource.SourceBuffer?
}

public abstract external class VideoTrackList : org.w3c.dom.events.EventTarget {
    /*primary*/ public constructor VideoTrackList()
    public open val length: kotlin.Int
        public open fun <get-length>(): kotlin.Int
    public open var onaddtrack: ((org.w3c.dom.TrackEvent) -> dynamic)?
        public open fun <get-onaddtrack>(): ((org.w3c.dom.TrackEvent) -> dynamic)?
        public open fun <set-onaddtrack>(/*0*/ <set-?>: ((org.w3c.dom.TrackEvent) -> dynamic)?): kotlin.Unit
    public open var onchange: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onchange>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onchange>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onremovetrack: ((org.w3c.dom.TrackEvent) -> dynamic)?
        public open fun <get-onremovetrack>(): ((org.w3c.dom.TrackEvent) -> dynamic)?
        public open fun <set-onremovetrack>(/*0*/ <set-?>: ((org.w3c.dom.TrackEvent) -> dynamic)?): kotlin.Unit
    public open val selectedIndex: kotlin.Int
        public open fun <get-selectedIndex>(): kotlin.Int
    public final fun getTrackById(/*0*/ id: kotlin.String): org.w3c.dom.VideoTrack?
}

public open external class WebSocket : org.w3c.dom.events.EventTarget {
    /*primary*/ public constructor WebSocket(/*0*/ url: kotlin.String, /*1*/ protocols: dynamic = ...)
    public final var binaryType: org.w3c.dom.BinaryType
        public final fun <get-binaryType>(): org.w3c.dom.BinaryType
        public final fun <set-binaryType>(/*0*/ <set-?>: org.w3c.dom.BinaryType): kotlin.Unit
    public open val bufferedAmount: kotlin.Number
        public open fun <get-bufferedAmount>(): kotlin.Number
    public open val extensions: kotlin.String
        public open fun <get-extensions>(): kotlin.String
    public final var onclose: ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <get-onclose>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <set-onclose>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public final var onerror: ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <get-onerror>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <set-onerror>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public final var onmessage: ((org.w3c.dom.MessageEvent) -> dynamic)?
        public final fun <get-onmessage>(): ((org.w3c.dom.MessageEvent) -> dynamic)?
        public final fun <set-onmessage>(/*0*/ <set-?>: ((org.w3c.dom.MessageEvent) -> dynamic)?): kotlin.Unit
    public final var onopen: ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <get-onopen>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <set-onopen>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open val protocol: kotlin.String
        public open fun <get-protocol>(): kotlin.String
    public open val readyState: kotlin.Short
        public open fun <get-readyState>(): kotlin.Short
    public open val url: kotlin.String
        public open fun <get-url>(): kotlin.String
    public final fun close(/*0*/ code: kotlin.Short = ..., /*1*/ reason: kotlin.String = ...): kotlin.Unit
    public final fun send(/*0*/ data: kotlin.String): kotlin.Unit
    public final fun send(/*0*/ data: org.khronos.webgl.ArrayBuffer): kotlin.Unit
    public final fun send(/*0*/ data: org.khronos.webgl.ArrayBufferView): kotlin.Unit
    public final fun send(/*0*/ data: org.w3c.files.Blob): kotlin.Unit

    public companion object Companion {
        public final val CLOSED: kotlin.Short
            public final fun <get-CLOSED>(): kotlin.Short
        public final val CLOSING: kotlin.Short
            public final fun <get-CLOSING>(): kotlin.Short
        public final val CONNECTING: kotlin.Short
            public final fun <get-CONNECTING>(): kotlin.Short
        public final val OPEN: kotlin.Short
            public final fun <get-OPEN>(): kotlin.Short
    }
}

public abstract external class Window : org.w3c.dom.events.EventTarget, org.w3c.dom.GlobalEventHandlers, org.w3c.dom.WindowEventHandlers, org.w3c.dom.WindowOrWorkerGlobalScope, org.w3c.dom.WindowSessionStorage, org.w3c.dom.WindowLocalStorage, org.w3c.performance.GlobalPerformance, org.w3c.dom.UnionMessagePortOrWindowProxy {
    /*primary*/ public constructor Window()
    public open val applicationCache: org.w3c.dom.ApplicationCache
        public open fun <get-applicationCache>(): org.w3c.dom.ApplicationCache
    public open val closed: kotlin.Boolean
        public open fun <get-closed>(): kotlin.Boolean
    public open val customElements: org.w3c.dom.CustomElementRegistry
        public open fun <get-customElements>(): org.w3c.dom.CustomElementRegistry
    public open val devicePixelRatio: kotlin.Double
        public open fun <get-devicePixelRatio>(): kotlin.Double
    public open val document: org.w3c.dom.Document
        public open fun <get-document>(): org.w3c.dom.Document
    public open val external: org.w3c.dom.External
        public open fun <get-external>(): org.w3c.dom.External
    public open val frameElement: org.w3c.dom.Element?
        public open fun <get-frameElement>(): org.w3c.dom.Element?
    public open val frames: org.w3c.dom.Window
        public open fun <get-frames>(): org.w3c.dom.Window
    public open val history: org.w3c.dom.History
        public open fun <get-history>(): org.w3c.dom.History
    public open val innerHeight: kotlin.Int
        public open fun <get-innerHeight>(): kotlin.Int
    public open val innerWidth: kotlin.Int
        public open fun <get-innerWidth>(): kotlin.Int
    public open val length: kotlin.Int
        public open fun <get-length>(): kotlin.Int
    public open val location: org.w3c.dom.Location
        public open fun <get-location>(): org.w3c.dom.Location
    public open val locationbar: org.w3c.dom.BarProp
        public open fun <get-locationbar>(): org.w3c.dom.BarProp
    public open val menubar: org.w3c.dom.BarProp
        public open fun <get-menubar>(): org.w3c.dom.BarProp
    public open var name: kotlin.String
        public open fun <get-name>(): kotlin.String
        public open fun <set-name>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val navigator: org.w3c.dom.Navigator
        public open fun <get-navigator>(): org.w3c.dom.Navigator
    public open var opener: kotlin.Any?
        public open fun <get-opener>(): kotlin.Any?
        public open fun <set-opener>(/*0*/ <set-?>: kotlin.Any?): kotlin.Unit
    public open val outerHeight: kotlin.Int
        public open fun <get-outerHeight>(): kotlin.Int
    public open val outerWidth: kotlin.Int
        public open fun <get-outerWidth>(): kotlin.Int
    public open val pageXOffset: kotlin.Double
        public open fun <get-pageXOffset>(): kotlin.Double
    public open val pageYOffset: kotlin.Double
        public open fun <get-pageYOffset>(): kotlin.Double
    public open val parent: org.w3c.dom.Window
        public open fun <get-parent>(): org.w3c.dom.Window
    public open val personalbar: org.w3c.dom.BarProp
        public open fun <get-personalbar>(): org.w3c.dom.BarProp
    public open val screen: org.w3c.dom.Screen
        public open fun <get-screen>(): org.w3c.dom.Screen
    public open val screenX: kotlin.Int
        public open fun <get-screenX>(): kotlin.Int
    public open val screenY: kotlin.Int
        public open fun <get-screenY>(): kotlin.Int
    public open val scrollX: kotlin.Double
        public open fun <get-scrollX>(): kotlin.Double
    public open val scrollY: kotlin.Double
        public open fun <get-scrollY>(): kotlin.Double
    public open val scrollbars: org.w3c.dom.BarProp
        public open fun <get-scrollbars>(): org.w3c.dom.BarProp
    public open val self: org.w3c.dom.Window
        public open fun <get-self>(): org.w3c.dom.Window
    public open var status: kotlin.String
        public open fun <get-status>(): kotlin.String
        public open fun <set-status>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val statusbar: org.w3c.dom.BarProp
        public open fun <get-statusbar>(): org.w3c.dom.BarProp
    public open val toolbar: org.w3c.dom.BarProp
        public open fun <get-toolbar>(): org.w3c.dom.BarProp
    public open val top: org.w3c.dom.Window
        public open fun <get-top>(): org.w3c.dom.Window
    public open val window: org.w3c.dom.Window
        public open fun <get-window>(): org.w3c.dom.Window
    public final fun alert(): kotlin.Unit
    public final fun alert(/*0*/ message: kotlin.String): kotlin.Unit
    public final fun blur(): kotlin.Unit
    public final fun cancelAnimationFrame(/*0*/ handle: kotlin.Int): kotlin.Unit
    public final fun captureEvents(): kotlin.Unit
    public final fun close(): kotlin.Unit
    public final fun confirm(/*0*/ message: kotlin.String = ...): kotlin.Boolean
    public final fun focus(): kotlin.Unit
    public final fun getComputedStyle(/*0*/ elt: org.w3c.dom.Element, /*1*/ pseudoElt: kotlin.String? = ...): org.w3c.dom.css.CSSStyleDeclaration
    public final fun matchMedia(/*0*/ query: kotlin.String): org.w3c.dom.MediaQueryList
    public final fun moveBy(/*0*/ x: kotlin.Int, /*1*/ y: kotlin.Int): kotlin.Unit
    public final fun moveTo(/*0*/ x: kotlin.Int, /*1*/ y: kotlin.Int): kotlin.Unit
    public final fun open(/*0*/ url: kotlin.String = ..., /*1*/ target: kotlin.String = ..., /*2*/ features: kotlin.String = ...): org.w3c.dom.Window?
    public final fun postMessage(/*0*/ message: kotlin.Any?, /*1*/ targetOrigin: kotlin.String, /*2*/ transfer: kotlin.Array<dynamic> = ...): kotlin.Unit
    public final fun print(): kotlin.Unit
    public final fun prompt(/*0*/ message: kotlin.String = ..., /*1*/ default: kotlin.String = ...): kotlin.String?
    public final fun releaseEvents(): kotlin.Unit
    public final fun requestAnimationFrame(/*0*/ callback: (kotlin.Double) -> kotlin.Unit): kotlin.Int
    public final fun resizeBy(/*0*/ x: kotlin.Int, /*1*/ y: kotlin.Int): kotlin.Unit
    public final fun resizeTo(/*0*/ x: kotlin.Int, /*1*/ y: kotlin.Int): kotlin.Unit
    public final fun scroll(/*0*/ x: kotlin.Double, /*1*/ y: kotlin.Double): kotlin.Unit
    public final fun scroll(/*0*/ options: org.w3c.dom.ScrollToOptions = ...): kotlin.Unit
    public final fun scrollBy(/*0*/ x: kotlin.Double, /*1*/ y: kotlin.Double): kotlin.Unit
    public final fun scrollBy(/*0*/ options: org.w3c.dom.ScrollToOptions = ...): kotlin.Unit
    public final fun scrollTo(/*0*/ x: kotlin.Double, /*1*/ y: kotlin.Double): kotlin.Unit
    public final fun scrollTo(/*0*/ options: org.w3c.dom.ScrollToOptions = ...): kotlin.Unit
    public final fun stop(): kotlin.Unit
}

public external interface WindowEventHandlers {
    public open var onafterprint: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onafterprint>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onafterprint>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onbeforeprint: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onbeforeprint>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onbeforeprint>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onbeforeunload: ((org.w3c.dom.BeforeUnloadEvent) -> kotlin.String?)?
        public open fun <get-onbeforeunload>(): ((org.w3c.dom.BeforeUnloadEvent) -> kotlin.String?)?
        public open fun <set-onbeforeunload>(/*0*/ value: ((org.w3c.dom.BeforeUnloadEvent) -> kotlin.String?)?): kotlin.Unit
    public open var onhashchange: ((org.w3c.dom.HashChangeEvent) -> dynamic)?
        public open fun <get-onhashchange>(): ((org.w3c.dom.HashChangeEvent) -> dynamic)?
        public open fun <set-onhashchange>(/*0*/ value: ((org.w3c.dom.HashChangeEvent) -> dynamic)?): kotlin.Unit
    public open var onlanguagechange: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onlanguagechange>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onlanguagechange>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onmessage: ((org.w3c.dom.MessageEvent) -> dynamic)?
        public open fun <get-onmessage>(): ((org.w3c.dom.MessageEvent) -> dynamic)?
        public open fun <set-onmessage>(/*0*/ value: ((org.w3c.dom.MessageEvent) -> dynamic)?): kotlin.Unit
    public open var onoffline: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onoffline>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onoffline>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var ononline: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-ononline>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-ononline>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onpagehide: ((org.w3c.dom.PageTransitionEvent) -> dynamic)?
        public open fun <get-onpagehide>(): ((org.w3c.dom.PageTransitionEvent) -> dynamic)?
        public open fun <set-onpagehide>(/*0*/ value: ((org.w3c.dom.PageTransitionEvent) -> dynamic)?): kotlin.Unit
    public open var onpageshow: ((org.w3c.dom.PageTransitionEvent) -> dynamic)?
        public open fun <get-onpageshow>(): ((org.w3c.dom.PageTransitionEvent) -> dynamic)?
        public open fun <set-onpageshow>(/*0*/ value: ((org.w3c.dom.PageTransitionEvent) -> dynamic)?): kotlin.Unit
    public open var onpopstate: ((org.w3c.dom.PopStateEvent) -> dynamic)?
        public open fun <get-onpopstate>(): ((org.w3c.dom.PopStateEvent) -> dynamic)?
        public open fun <set-onpopstate>(/*0*/ value: ((org.w3c.dom.PopStateEvent) -> dynamic)?): kotlin.Unit
    public open var onrejectionhandled: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onrejectionhandled>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onrejectionhandled>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onstorage: ((org.w3c.dom.StorageEvent) -> dynamic)?
        public open fun <get-onstorage>(): ((org.w3c.dom.StorageEvent) -> dynamic)?
        public open fun <set-onstorage>(/*0*/ value: ((org.w3c.dom.StorageEvent) -> dynamic)?): kotlin.Unit
    public open var onunhandledrejection: ((org.w3c.dom.PromiseRejectionEvent) -> dynamic)?
        public open fun <get-onunhandledrejection>(): ((org.w3c.dom.PromiseRejectionEvent) -> dynamic)?
        public open fun <set-onunhandledrejection>(/*0*/ value: ((org.w3c.dom.PromiseRejectionEvent) -> dynamic)?): kotlin.Unit
    public open var onunload: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onunload>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onunload>(/*0*/ value: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
}

public external interface WindowLocalStorage {
    public abstract val localStorage: org.w3c.dom.Storage
        public abstract fun <get-localStorage>(): org.w3c.dom.Storage
}

public external interface WindowOrWorkerGlobalScope {
    public abstract val caches: org.w3c.workers.CacheStorage
        public abstract fun <get-caches>(): org.w3c.workers.CacheStorage
    public abstract val origin: kotlin.String
        public abstract fun <get-origin>(): kotlin.String
    public abstract fun atob(/*0*/ data: kotlin.String): kotlin.String
    public abstract fun btoa(/*0*/ data: kotlin.String): kotlin.String
    public abstract fun clearInterval(/*0*/ handle: kotlin.Int = ...): kotlin.Unit
    public abstract fun clearTimeout(/*0*/ handle: kotlin.Int = ...): kotlin.Unit
    public abstract fun createImageBitmap(/*0*/ image: org.w3c.dom.ImageBitmapSource, /*1*/ sx: kotlin.Int, /*2*/ sy: kotlin.Int, /*3*/ sw: kotlin.Int, /*4*/ sh: kotlin.Int, /*5*/ options: org.w3c.dom.ImageBitmapOptions = ...): kotlin.js.Promise<org.w3c.dom.ImageBitmap>
    public abstract fun createImageBitmap(/*0*/ image: org.w3c.dom.ImageBitmapSource, /*1*/ options: org.w3c.dom.ImageBitmapOptions = ...): kotlin.js.Promise<org.w3c.dom.ImageBitmap>
    public abstract fun fetch(/*0*/ input: dynamic, /*1*/ init: org.w3c.fetch.RequestInit = ...): kotlin.js.Promise<org.w3c.fetch.Response>
    public abstract fun setInterval(/*0*/ handler: dynamic, /*1*/ timeout: kotlin.Int = ..., /*2*/ vararg arguments: kotlin.Any? /*kotlin.Array<out kotlin.Any?>*/): kotlin.Int
    public abstract fun setTimeout(/*0*/ handler: dynamic, /*1*/ timeout: kotlin.Int = ..., /*2*/ vararg arguments: kotlin.Any? /*kotlin.Array<out kotlin.Any?>*/): kotlin.Int
}

public external interface WindowSessionStorage {
    public abstract val sessionStorage: org.w3c.dom.Storage
        public abstract fun <get-sessionStorage>(): org.w3c.dom.Storage
}

public open external class Worker : org.w3c.dom.events.EventTarget, org.w3c.dom.AbstractWorker {
    /*primary*/ public constructor Worker(/*0*/ scriptURL: kotlin.String, /*1*/ options: org.w3c.dom.WorkerOptions = ...)
    public open override /*1*/ var onerror: ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <get-onerror>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open override /*1*/ fun <set-onerror>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public final var onmessage: ((org.w3c.dom.MessageEvent) -> dynamic)?
        public final fun <get-onmessage>(): ((org.w3c.dom.MessageEvent) -> dynamic)?
        public final fun <set-onmessage>(/*0*/ <set-?>: ((org.w3c.dom.MessageEvent) -> dynamic)?): kotlin.Unit
    public final fun postMessage(/*0*/ message: kotlin.Any?, /*1*/ transfer: kotlin.Array<dynamic> = ...): kotlin.Unit
    public final fun terminate(): kotlin.Unit
}

public abstract external class WorkerGlobalScope : org.w3c.dom.events.EventTarget, org.w3c.dom.WindowOrWorkerGlobalScope, org.w3c.performance.GlobalPerformance {
    /*primary*/ public constructor WorkerGlobalScope()
    public open val location: org.w3c.dom.WorkerLocation
        public open fun <get-location>(): org.w3c.dom.WorkerLocation
    public open val navigator: org.w3c.dom.WorkerNavigator
        public open fun <get-navigator>(): org.w3c.dom.WorkerNavigator
    public open var onerror: ((dynamic, kotlin.String, kotlin.Int, kotlin.Int, kotlin.Any?) -> dynamic)?
        public open fun <get-onerror>(): ((dynamic, kotlin.String, kotlin.Int, kotlin.Int, kotlin.Any?) -> dynamic)?
        public open fun <set-onerror>(/*0*/ <set-?>: ((dynamic, kotlin.String, kotlin.Int, kotlin.Int, kotlin.Any?) -> dynamic)?): kotlin.Unit
    public open var onlanguagechange: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onlanguagechange>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onlanguagechange>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onoffline: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onoffline>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onoffline>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var ononline: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-ononline>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-ononline>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onrejectionhandled: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onrejectionhandled>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onrejectionhandled>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onunhandledrejection: ((org.w3c.dom.PromiseRejectionEvent) -> dynamic)?
        public open fun <get-onunhandledrejection>(): ((org.w3c.dom.PromiseRejectionEvent) -> dynamic)?
        public open fun <set-onunhandledrejection>(/*0*/ <set-?>: ((org.w3c.dom.PromiseRejectionEvent) -> dynamic)?): kotlin.Unit
    public open val self: org.w3c.dom.WorkerGlobalScope
        public open fun <get-self>(): org.w3c.dom.WorkerGlobalScope
    public final fun importScripts(/*0*/ vararg urls: kotlin.String /*kotlin.Array<out kotlin.String>*/): kotlin.Unit
}

public abstract external class WorkerLocation {
    /*primary*/ public constructor WorkerLocation()
    public open val hash: kotlin.String
        public open fun <get-hash>(): kotlin.String
    public open val host: kotlin.String
        public open fun <get-host>(): kotlin.String
    public open val hostname: kotlin.String
        public open fun <get-hostname>(): kotlin.String
    public open val href: kotlin.String
        public open fun <get-href>(): kotlin.String
    public open val origin: kotlin.String
        public open fun <get-origin>(): kotlin.String
    public open val pathname: kotlin.String
        public open fun <get-pathname>(): kotlin.String
    public open val port: kotlin.String
        public open fun <get-port>(): kotlin.String
    public open val protocol: kotlin.String
        public open fun <get-protocol>(): kotlin.String
    public open val search: kotlin.String
        public open fun <get-search>(): kotlin.String
}

public abstract external class WorkerNavigator : org.w3c.dom.NavigatorID, org.w3c.dom.NavigatorLanguage, org.w3c.dom.NavigatorOnLine, org.w3c.dom.NavigatorConcurrentHardware {
    /*primary*/ public constructor WorkerNavigator()
    public open val serviceWorker: org.w3c.workers.ServiceWorkerContainer
        public open fun <get-serviceWorker>(): org.w3c.workers.ServiceWorkerContainer
}

public external interface WorkerOptions {
    public open var credentials: org.w3c.fetch.RequestCredentials?
        public open fun <get-credentials>(): org.w3c.fetch.RequestCredentials?
        public open fun <set-credentials>(/*0*/ value: org.w3c.fetch.RequestCredentials?): kotlin.Unit
    public open var type: org.w3c.dom.WorkerType?
        public open fun <get-type>(): org.w3c.dom.WorkerType?
        public open fun <set-type>(/*0*/ value: org.w3c.dom.WorkerType?): kotlin.Unit
}

public external interface WorkerType {

    public companion object Companion {
    }
}

public open external class XMLDocument : org.w3c.dom.Document {
    /*primary*/ public constructor XMLDocument()

    public companion object Companion {
        public final val ATTRIBUTE_NODE: kotlin.Short
            public final fun <get-ATTRIBUTE_NODE>(): kotlin.Short
        public final val CDATA_SECTION_NODE: kotlin.Short
            public final fun <get-CDATA_SECTION_NODE>(): kotlin.Short
        public final val COMMENT_NODE: kotlin.Short
            public final fun <get-COMMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_FRAGMENT_NODE: kotlin.Short
            public final fun <get-DOCUMENT_FRAGMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_NODE: kotlin.Short
            public final fun <get-DOCUMENT_NODE>(): kotlin.Short
        public final val DOCUMENT_POSITION_CONTAINED_BY: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_CONTAINED_BY>(): kotlin.Short
        public final val DOCUMENT_POSITION_CONTAINS: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_CONTAINS>(): kotlin.Short
        public final val DOCUMENT_POSITION_DISCONNECTED: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_DISCONNECTED>(): kotlin.Short
        public final val DOCUMENT_POSITION_FOLLOWING: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_FOLLOWING>(): kotlin.Short
        public final val DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC>(): kotlin.Short
        public final val DOCUMENT_POSITION_PRECEDING: kotlin.Short
            public final fun <get-DOCUMENT_POSITION_PRECEDING>(): kotlin.Short
        public final val DOCUMENT_TYPE_NODE: kotlin.Short
            public final fun <get-DOCUMENT_TYPE_NODE>(): kotlin.Short
        public final val ELEMENT_NODE: kotlin.Short
            public final fun <get-ELEMENT_NODE>(): kotlin.Short
        public final val ENTITY_NODE: kotlin.Short
            public final fun <get-ENTITY_NODE>(): kotlin.Short
        public final val ENTITY_REFERENCE_NODE: kotlin.Short
            public final fun <get-ENTITY_REFERENCE_NODE>(): kotlin.Short
        public final val NOTATION_NODE: kotlin.Short
            public final fun <get-NOTATION_NODE>(): kotlin.Short
        public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short
            public final fun <get-PROCESSING_INSTRUCTION_NODE>(): kotlin.Short
        public final val TEXT_NODE: kotlin.Short
            public final fun <get-TEXT_NODE>(): kotlin.Short
    }
}
@kotlin.Deprecated(message = "Use UnionMessagePortOrWindowProxy instead.", replaceWith = kotlin.ReplaceWith(expression = "UnionMessagePortOrWindowProxy", imports = {})) public typealias UnionMessagePortOrWindow = org.w3c.dom.UnionMessagePortOrWindowProxy
