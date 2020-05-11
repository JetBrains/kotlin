
public abstract external class HTMLMediaElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLMediaElement()
    public open val audioTracks: org.w3c.dom.AudioTrackList
        public open fun <get-audioTracks>(): org.w3c.dom.AudioTrackList
    public open var autoplay: kotlin.Boolean
        public open fun <get-autoplay>(): kotlin.Boolean
        public open fun <set-autoplay>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open val buffered: org.w3c.dom.TimeRanges
        public open fun <get-buffered>(): org.w3c.dom.TimeRanges
    public open var controls: kotlin.Boolean
        public open fun <get-controls>(): kotlin.Boolean
        public open fun <set-controls>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open var crossOrigin: kotlin.String?
        public open fun <get-crossOrigin>(): kotlin.String?
        public open fun <set-crossOrigin>(/*0*/ <set-?>: kotlin.String?): kotlin.Unit
    public open val currentSrc: kotlin.String
        public open fun <get-currentSrc>(): kotlin.String
    public open var currentTime: kotlin.Double
        public open fun <get-currentTime>(): kotlin.Double
        public open fun <set-currentTime>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open var defaultMuted: kotlin.Boolean
        public open fun <get-defaultMuted>(): kotlin.Boolean
        public open fun <set-defaultMuted>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open var defaultPlaybackRate: kotlin.Double
        public open fun <get-defaultPlaybackRate>(): kotlin.Double
        public open fun <set-defaultPlaybackRate>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open val duration: kotlin.Double
        public open fun <get-duration>(): kotlin.Double
    public open val ended: kotlin.Boolean
        public open fun <get-ended>(): kotlin.Boolean
    public open val error: org.w3c.dom.MediaError?
        public open fun <get-error>(): org.w3c.dom.MediaError?
    public open var loop: kotlin.Boolean
        public open fun <get-loop>(): kotlin.Boolean
        public open fun <set-loop>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open val mediaKeys: org.w3c.dom.encryptedmedia.MediaKeys?
        public open fun <get-mediaKeys>(): org.w3c.dom.encryptedmedia.MediaKeys?
    public open var muted: kotlin.Boolean
        public open fun <get-muted>(): kotlin.Boolean
        public open fun <set-muted>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open val networkState: kotlin.Short
        public open fun <get-networkState>(): kotlin.Short
    public open var onencrypted: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onencrypted>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onencrypted>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onwaitingforkey: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onwaitingforkey>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onwaitingforkey>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open val paused: kotlin.Boolean
        public open fun <get-paused>(): kotlin.Boolean
    public open var playbackRate: kotlin.Double
        public open fun <get-playbackRate>(): kotlin.Double
        public open fun <set-playbackRate>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open val played: org.w3c.dom.TimeRanges
        public open fun <get-played>(): org.w3c.dom.TimeRanges
    public open var preload: kotlin.String
        public open fun <get-preload>(): kotlin.String
        public open fun <set-preload>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val readyState: kotlin.Short
        public open fun <get-readyState>(): kotlin.Short
    public open val seekable: org.w3c.dom.TimeRanges
        public open fun <get-seekable>(): org.w3c.dom.TimeRanges
    public open val seeking: kotlin.Boolean
        public open fun <get-seeking>(): kotlin.Boolean
    public open var src: kotlin.String
        public open fun <get-src>(): kotlin.String
        public open fun <set-src>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var srcObject: org.w3c.dom.MediaProvider?
        public open fun <get-srcObject>(): org.w3c.dom.MediaProvider?
        public open fun <set-srcObject>(/*0*/ <set-?>: org.w3c.dom.MediaProvider?): kotlin.Unit
    public open val textTracks: org.w3c.dom.TextTrackList
        public open fun <get-textTracks>(): org.w3c.dom.TextTrackList
    public open val videoTracks: org.w3c.dom.VideoTrackList
        public open fun <get-videoTracks>(): org.w3c.dom.VideoTrackList
    public open var volume: kotlin.Double
        public open fun <get-volume>(): kotlin.Double
        public open fun <set-volume>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public final fun addTextTrack(/*0*/ kind: org.w3c.dom.TextTrackKind, /*1*/ label: kotlin.String = ..., /*2*/ language: kotlin.String = ...): org.w3c.dom.TextTrack
    public final fun canPlayType(/*0*/ type: kotlin.String): org.w3c.dom.CanPlayTypeResult
    public final fun fastSeek(/*0*/ time: kotlin.Double): kotlin.Unit
    public final fun getStartDate(): dynamic
    public final fun load(): kotlin.Unit
    public final fun pause(): kotlin.Unit
    public final fun play(): kotlin.js.Promise<kotlin.Unit>
    public final fun setMediaKeys(/*0*/ mediaKeys: org.w3c.dom.encryptedmedia.MediaKeys?): kotlin.js.Promise<kotlin.Unit>

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
        public final val HAVE_CURRENT_DATA: kotlin.Short
            public final fun <get-HAVE_CURRENT_DATA>(): kotlin.Short
        public final val HAVE_ENOUGH_DATA: kotlin.Short
            public final fun <get-HAVE_ENOUGH_DATA>(): kotlin.Short
        public final val HAVE_FUTURE_DATA: kotlin.Short
            public final fun <get-HAVE_FUTURE_DATA>(): kotlin.Short
        public final val HAVE_METADATA: kotlin.Short
            public final fun <get-HAVE_METADATA>(): kotlin.Short
        public final val HAVE_NOTHING: kotlin.Short
            public final fun <get-HAVE_NOTHING>(): kotlin.Short
        public final val NETWORK_EMPTY: kotlin.Short
            public final fun <get-NETWORK_EMPTY>(): kotlin.Short
        public final val NETWORK_IDLE: kotlin.Short
            public final fun <get-NETWORK_IDLE>(): kotlin.Short
        public final val NETWORK_LOADING: kotlin.Short
            public final fun <get-NETWORK_LOADING>(): kotlin.Short
        public final val NETWORK_NO_SOURCE: kotlin.Short
            public final fun <get-NETWORK_NO_SOURCE>(): kotlin.Short
        public final val NOTATION_NODE: kotlin.Short
            public final fun <get-NOTATION_NODE>(): kotlin.Short
        public final val PROCESSING_INSTRUCTION_NODE: kotlin.Short
            public final fun <get-PROCESSING_INSTRUCTION_NODE>(): kotlin.Short
        public final val TEXT_NODE: kotlin.Short
            public final fun <get-TEXT_NODE>(): kotlin.Short
    }
}

public abstract external class HTMLMenuElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLMenuElement()
    public open var compact: kotlin.Boolean
        public open fun <get-compact>(): kotlin.Boolean
        public open fun <set-compact>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open var label: kotlin.String
        public open fun <get-label>(): kotlin.String
        public open fun <set-label>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var type: kotlin.String
        public open fun <get-type>(): kotlin.String
        public open fun <set-type>(/*0*/ <set-?>: kotlin.String): kotlin.Unit

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

public abstract external class HTMLMenuItemElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLMenuItemElement()
    public open var checked: kotlin.Boolean
        public open fun <get-checked>(): kotlin.Boolean
        public open fun <set-checked>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open var default: kotlin.Boolean
        public open fun <get-default>(): kotlin.Boolean
        public open fun <set-default>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open var disabled: kotlin.Boolean
        public open fun <get-disabled>(): kotlin.Boolean
        public open fun <set-disabled>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open var icon: kotlin.String
        public open fun <get-icon>(): kotlin.String
        public open fun <set-icon>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var label: kotlin.String
        public open fun <get-label>(): kotlin.String
        public open fun <set-label>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var radiogroup: kotlin.String
        public open fun <get-radiogroup>(): kotlin.String
        public open fun <set-radiogroup>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var type: kotlin.String
        public open fun <get-type>(): kotlin.String
        public open fun <set-type>(/*0*/ <set-?>: kotlin.String): kotlin.Unit

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

public abstract external class HTMLMetaElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLMetaElement()
    public open var content: kotlin.String
        public open fun <get-content>(): kotlin.String
        public open fun <set-content>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var httpEquiv: kotlin.String
        public open fun <get-httpEquiv>(): kotlin.String
        public open fun <set-httpEquiv>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var name: kotlin.String
        public open fun <get-name>(): kotlin.String
        public open fun <set-name>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var scheme: kotlin.String
        public open fun <get-scheme>(): kotlin.String
        public open fun <set-scheme>(/*0*/ <set-?>: kotlin.String): kotlin.Unit

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

public abstract external class HTMLMeterElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLMeterElement()
    public open var high: kotlin.Double
        public open fun <get-high>(): kotlin.Double
        public open fun <set-high>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open val labels: org.w3c.dom.NodeList
        public open fun <get-labels>(): org.w3c.dom.NodeList
    public open var low: kotlin.Double
        public open fun <get-low>(): kotlin.Double
        public open fun <set-low>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open var max: kotlin.Double
        public open fun <get-max>(): kotlin.Double
        public open fun <set-max>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open var min: kotlin.Double
        public open fun <get-min>(): kotlin.Double
        public open fun <set-min>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open var optimum: kotlin.Double
        public open fun <get-optimum>(): kotlin.Double
        public open fun <set-optimum>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open var value: kotlin.Double
        public open fun <get-value>(): kotlin.Double
        public open fun <set-value>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit

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

public abstract external class HTMLModElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLModElement()
    public open var cite: kotlin.String
        public open fun <get-cite>(): kotlin.String
        public open fun <set-cite>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var dateTime: kotlin.String
        public open fun <get-dateTime>(): kotlin.String
        public open fun <set-dateTime>(/*0*/ <set-?>: kotlin.String): kotlin.Unit

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

public abstract external class HTMLOListElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLOListElement()
    public open var compact: kotlin.Boolean
        public open fun <get-compact>(): kotlin.Boolean
        public open fun <set-compact>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open var reversed: kotlin.Boolean
        public open fun <get-reversed>(): kotlin.Boolean
        public open fun <set-reversed>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open var start: kotlin.Int
        public open fun <get-start>(): kotlin.Int
        public open fun <set-start>(/*0*/ <set-?>: kotlin.Int): kotlin.Unit
    public open var type: kotlin.String
        public open fun <get-type>(): kotlin.String
        public open fun <set-type>(/*0*/ <set-?>: kotlin.String): kotlin.Unit

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

public abstract external class HTMLObjectElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLObjectElement()
    public open var align: kotlin.String
        public open fun <get-align>(): kotlin.String
        public open fun <set-align>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var archive: kotlin.String
        public open fun <get-archive>(): kotlin.String
        public open fun <set-archive>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var border: kotlin.String
        public open fun <get-border>(): kotlin.String
        public open fun <set-border>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var code: kotlin.String
        public open fun <get-code>(): kotlin.String
        public open fun <set-code>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var codeBase: kotlin.String
        public open fun <get-codeBase>(): kotlin.String
        public open fun <set-codeBase>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var codeType: kotlin.String
        public open fun <get-codeType>(): kotlin.String
        public open fun <set-codeType>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val contentDocument: org.w3c.dom.Document?
        public open fun <get-contentDocument>(): org.w3c.dom.Document?
    public open val contentWindow: org.w3c.dom.Window?
        public open fun <get-contentWindow>(): org.w3c.dom.Window?
    public open var data: kotlin.String
        public open fun <get-data>(): kotlin.String
        public open fun <set-data>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var declare: kotlin.Boolean
        public open fun <get-declare>(): kotlin.Boolean
        public open fun <set-declare>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open val form: org.w3c.dom.HTMLFormElement?
        public open fun <get-form>(): org.w3c.dom.HTMLFormElement?
    public open var height: kotlin.String
        public open fun <get-height>(): kotlin.String
        public open fun <set-height>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var hspace: kotlin.Int
        public open fun <get-hspace>(): kotlin.Int
        public open fun <set-hspace>(/*0*/ <set-?>: kotlin.Int): kotlin.Unit
    public open var name: kotlin.String
        public open fun <get-name>(): kotlin.String
        public open fun <set-name>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var standby: kotlin.String
        public open fun <get-standby>(): kotlin.String
        public open fun <set-standby>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var type: kotlin.String
        public open fun <get-type>(): kotlin.String
        public open fun <set-type>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var typeMustMatch: kotlin.Boolean
        public open fun <get-typeMustMatch>(): kotlin.Boolean
        public open fun <set-typeMustMatch>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open var useMap: kotlin.String
        public open fun <get-useMap>(): kotlin.String
        public open fun <set-useMap>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val validationMessage: kotlin.String
        public open fun <get-validationMessage>(): kotlin.String
    public open val validity: org.w3c.dom.ValidityState
        public open fun <get-validity>(): org.w3c.dom.ValidityState
    public open var vspace: kotlin.Int
        public open fun <get-vspace>(): kotlin.Int
        public open fun <set-vspace>(/*0*/ <set-?>: kotlin.Int): kotlin.Unit
    public open var width: kotlin.String
        public open fun <get-width>(): kotlin.String
        public open fun <set-width>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val willValidate: kotlin.Boolean
        public open fun <get-willValidate>(): kotlin.Boolean
    public final fun checkValidity(): kotlin.Boolean
    public final fun getSVGDocument(): org.w3c.dom.Document?
    public final fun reportValidity(): kotlin.Boolean
    public final fun setCustomValidity(/*0*/ error: kotlin.String): kotlin.Unit

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

public abstract external class HTMLOptGroupElement : org.w3c.dom.HTMLElement, org.w3c.dom.UnionHTMLOptGroupElementOrHTMLOptionElement {
    /*primary*/ public constructor HTMLOptGroupElement()
    public open var disabled: kotlin.Boolean
        public open fun <get-disabled>(): kotlin.Boolean
        public open fun <set-disabled>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open var label: kotlin.String
        public open fun <get-label>(): kotlin.String
        public open fun <set-label>(/*0*/ <set-?>: kotlin.String): kotlin.Unit

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

public abstract external class HTMLOptionElement : org.w3c.dom.HTMLElement, org.w3c.dom.UnionHTMLOptGroupElementOrHTMLOptionElement {
    /*primary*/ public constructor HTMLOptionElement()
    public open var defaultSelected: kotlin.Boolean
        public open fun <get-defaultSelected>(): kotlin.Boolean
        public open fun <set-defaultSelected>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open var disabled: kotlin.Boolean
        public open fun <get-disabled>(): kotlin.Boolean
        public open fun <set-disabled>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open val form: org.w3c.dom.HTMLFormElement?
        public open fun <get-form>(): org.w3c.dom.HTMLFormElement?
    public open val index: kotlin.Int
        public open fun <get-index>(): kotlin.Int
    public open var label: kotlin.String
        public open fun <get-label>(): kotlin.String
        public open fun <set-label>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var selected: kotlin.Boolean
        public open fun <get-selected>(): kotlin.Boolean
        public open fun <set-selected>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public open var text: kotlin.String
        public open fun <get-text>(): kotlin.String
        public open fun <set-text>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var value: kotlin.String
        public open fun <get-value>(): kotlin.String
        public open fun <set-value>(/*0*/ <set-?>: kotlin.String): kotlin.Unit

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

public abstract external class HTMLOptionsCollection : org.w3c.dom.HTMLCollection {
    /*primary*/ public constructor HTMLOptionsCollection()
    public open override /*1*/ var length: kotlin.Int
        public open override /*1*/ fun <get-length>(): kotlin.Int
        public open fun <set-length>(/*0*/ <set-?>: kotlin.Int): kotlin.Unit
    public open var selectedIndex: kotlin.Int
        public open fun <get-selectedIndex>(): kotlin.Int
        public open fun <set-selectedIndex>(/*0*/ <set-?>: kotlin.Int): kotlin.Unit
    public final fun add(/*0*/ element: org.w3c.dom.UnionHTMLOptGroupElementOrHTMLOptionElement, /*1*/ before: dynamic = ...): kotlin.Unit
    public final fun remove(/*0*/ index: kotlin.Int): kotlin.Unit
}

public external interface HTMLOrSVGImageElement : org.w3c.dom.CanvasImageSource {
}

public external interface HTMLOrSVGScriptElement {
}

public abstract external class HTMLOutputElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLOutputElement()
    public open var defaultValue: kotlin.String
        public open fun <get-defaultValue>(): kotlin.String
        public open fun <set-defaultValue>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val form: org.w3c.dom.HTMLFormElement?
        public open fun <get-form>(): org.w3c.dom.HTMLFormElement?
    public open val htmlFor: org.w3c.dom.DOMTokenList
        public open fun <get-htmlFor>(): org.w3c.dom.DOMTokenList
    public open val labels: org.w3c.dom.NodeList
        public open fun <get-labels>(): org.w3c.dom.NodeList
    public open var name: kotlin.String
        public open fun <get-name>(): kotlin.String
        public open fun <set-name>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val type: kotlin.String
        public open fun <get-type>(): kotlin.String
    public open val validationMessage: kotlin.String
        public open fun <get-validationMessage>(): kotlin.String
    public open val validity: org.w3c.dom.ValidityState
        public open fun <get-validity>(): org.w3c.dom.ValidityState
    public open var value: kotlin.String
        public open fun <get-value>(): kotlin.String
        public open fun <set-value>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val willValidate: kotlin.Boolean
        public open fun <get-willValidate>(): kotlin.Boolean
    public final fun checkValidity(): kotlin.Boolean
    public final fun reportValidity(): kotlin.Boolean
    public final fun setCustomValidity(/*0*/ error: kotlin.String): kotlin.Unit

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

public abstract external class HTMLParagraphElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLParagraphElement()
    public open var align: kotlin.String
        public open fun <get-align>(): kotlin.String
        public open fun <set-align>(/*0*/ <set-?>: kotlin.String): kotlin.Unit

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

public abstract external class HTMLParamElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLParamElement()
    public open var name: kotlin.String
        public open fun <get-name>(): kotlin.String
        public open fun <set-name>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var type: kotlin.String
        public open fun <get-type>(): kotlin.String
        public open fun <set-type>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var value: kotlin.String
        public open fun <get-value>(): kotlin.String
        public open fun <set-value>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open var valueType: kotlin.String
        public open fun <get-valueType>(): kotlin.String
        public open fun <set-valueType>(/*0*/ <set-?>: kotlin.String): kotlin.Unit

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

public abstract external class HTMLPictureElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLPictureElement()

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

public abstract external class HTMLPreElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLPreElement()
    public open var width: kotlin.Int
        public open fun <get-width>(): kotlin.Int
        public open fun <set-width>(/*0*/ <set-?>: kotlin.Int): kotlin.Unit

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

public abstract external class HTMLProgressElement : org.w3c.dom.HTMLElement {
    /*primary*/ public constructor HTMLProgressElement()
    public open val labels: org.w3c.dom.NodeList
        public open fun <get-labels>(): org.w3c.dom.NodeList
    public open var max: kotlin.Double
        public open fun <get-max>(): kotlin.Double
        public open fun <set-max>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open val position: kotlin.Double
        public open fun <get-position>(): kotlin.Double
    public open var value: kotlin.Double
        public open fun <get-value>(): kotlin.Double
        public open fun <set-value>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit

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
