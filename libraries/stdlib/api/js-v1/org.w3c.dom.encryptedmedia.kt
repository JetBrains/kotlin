/*∆*/ public val org.w3c.dom.encryptedmedia.MediaKeyStatus.Companion.EXPIRED: org.w3c.dom.encryptedmedia.MediaKeyStatus { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.encryptedmedia.MediaKeyMessageType.Companion.INDIVIDUALIZATION_REQUEST: org.w3c.dom.encryptedmedia.MediaKeyMessageType { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.encryptedmedia.MediaKeyStatus.Companion.INTERNAL_ERROR: org.w3c.dom.encryptedmedia.MediaKeyStatus { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.encryptedmedia.MediaKeyMessageType.Companion.LICENSE_RELEASE: org.w3c.dom.encryptedmedia.MediaKeyMessageType { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.encryptedmedia.MediaKeyMessageType.Companion.LICENSE_RENEWAL: org.w3c.dom.encryptedmedia.MediaKeyMessageType { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.encryptedmedia.MediaKeyMessageType.Companion.LICENSE_REQUEST: org.w3c.dom.encryptedmedia.MediaKeyMessageType { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.encryptedmedia.MediaKeysRequirement.Companion.NOT_ALLOWED: org.w3c.dom.encryptedmedia.MediaKeysRequirement { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.encryptedmedia.MediaKeysRequirement.Companion.OPTIONAL: org.w3c.dom.encryptedmedia.MediaKeysRequirement { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.encryptedmedia.MediaKeyStatus.Companion.OUTPUT_DOWNSCALED: org.w3c.dom.encryptedmedia.MediaKeyStatus { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.encryptedmedia.MediaKeyStatus.Companion.OUTPUT_RESTRICTED: org.w3c.dom.encryptedmedia.MediaKeyStatus { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.encryptedmedia.MediaKeySessionType.Companion.PERSISTENT_LICENSE: org.w3c.dom.encryptedmedia.MediaKeySessionType { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.encryptedmedia.MediaKeyStatus.Companion.RELEASED: org.w3c.dom.encryptedmedia.MediaKeyStatus { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.encryptedmedia.MediaKeysRequirement.Companion.REQUIRED: org.w3c.dom.encryptedmedia.MediaKeysRequirement { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.encryptedmedia.MediaKeyStatus.Companion.STATUS_PENDING: org.w3c.dom.encryptedmedia.MediaKeyStatus { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.encryptedmedia.MediaKeySessionType.Companion.TEMPORARY: org.w3c.dom.encryptedmedia.MediaKeySessionType { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.encryptedmedia.MediaKeyStatus.Companion.USABLE: org.w3c.dom.encryptedmedia.MediaKeyStatus { get; }
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun MediaEncryptedEventInit(initDataType: kotlin.String? = ..., initData: org.khronos.webgl.ArrayBuffer? = ..., bubbles: kotlin.Boolean? = ..., cancelable: kotlin.Boolean? = ..., composed: kotlin.Boolean? = ...): org.w3c.dom.encryptedmedia.MediaEncryptedEventInit
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun MediaKeyMessageEventInit(messageType: org.w3c.dom.encryptedmedia.MediaKeyMessageType?, message: org.khronos.webgl.ArrayBuffer?, bubbles: kotlin.Boolean? = ..., cancelable: kotlin.Boolean? = ..., composed: kotlin.Boolean? = ...): org.w3c.dom.encryptedmedia.MediaKeyMessageEventInit
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun MediaKeySystemConfiguration(label: kotlin.String? = ..., initDataTypes: kotlin.Array<kotlin.String>? = ..., audioCapabilities: kotlin.Array<org.w3c.dom.encryptedmedia.MediaKeySystemMediaCapability>? = ..., videoCapabilities: kotlin.Array<org.w3c.dom.encryptedmedia.MediaKeySystemMediaCapability>? = ..., distinctiveIdentifier: org.w3c.dom.encryptedmedia.MediaKeysRequirement? = ..., persistentState: org.w3c.dom.encryptedmedia.MediaKeysRequirement? = ..., sessionTypes: kotlin.Array<kotlin.String>? = ...): org.w3c.dom.encryptedmedia.MediaKeySystemConfiguration
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun MediaKeySystemMediaCapability(contentType: kotlin.String? = ..., robustness: kotlin.String? = ...): org.w3c.dom.encryptedmedia.MediaKeySystemMediaCapability
/*∆*/ 
/*∆*/ public open external class MediaEncryptedEvent : org.w3c.dom.events.Event {
/*∆*/     public constructor MediaEncryptedEvent(type: kotlin.String, eventInitDict: org.w3c.dom.encryptedmedia.MediaEncryptedEventInit = ...)
/*∆*/ 
/*∆*/     public open val initData: org.khronos.webgl.ArrayBuffer? { get; }
/*∆*/ 
/*∆*/     public open val initDataType: kotlin.String { get; }
/*∆*/ 
/*∆*/     public companion object of MediaEncryptedEvent {
/*∆*/         public final val AT_TARGET: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val BUBBLING_PHASE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CAPTURING_PHASE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NONE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface MediaEncryptedEventInit : org.w3c.dom.EventInit {
/*∆*/     public open var initData: org.khronos.webgl.ArrayBuffer? { get; set; }
/*∆*/ 
/*∆*/     public open var initDataType: kotlin.String? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class MediaKeyMessageEvent : org.w3c.dom.events.Event {
/*∆*/     public constructor MediaKeyMessageEvent(type: kotlin.String, eventInitDict: org.w3c.dom.encryptedmedia.MediaKeyMessageEventInit)
/*∆*/ 
/*∆*/     public open val message: org.khronos.webgl.ArrayBuffer { get; }
/*∆*/ 
/*∆*/     public open val messageType: org.w3c.dom.encryptedmedia.MediaKeyMessageType { get; }
/*∆*/ 
/*∆*/     public companion object of MediaKeyMessageEvent {
/*∆*/         public final val AT_TARGET: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val BUBBLING_PHASE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CAPTURING_PHASE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NONE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface MediaKeyMessageEventInit : org.w3c.dom.EventInit {
/*∆*/     public abstract var message: org.khronos.webgl.ArrayBuffer? { get; set; }
/*∆*/ 
/*∆*/     public abstract var messageType: org.w3c.dom.encryptedmedia.MediaKeyMessageType? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ @kotlin.js.JsName(name = "null")
/*∆*/ public external interface MediaKeyMessageType {
/*∆*/     public companion object of MediaKeyMessageType {
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class MediaKeySession : org.w3c.dom.events.EventTarget {
/*∆*/     public constructor MediaKeySession()
/*∆*/ 
/*∆*/     public open val closed: kotlin.js.Promise<kotlin.Unit> { get; }
/*∆*/ 
/*∆*/     public open val expiration: kotlin.Double { get; }
/*∆*/ 
/*∆*/     public open val keyStatuses: org.w3c.dom.encryptedmedia.MediaKeyStatusMap { get; }
/*∆*/ 
/*∆*/     public open var onkeystatuseschange: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open var onmessage: ((org.w3c.dom.MessageEvent) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open val sessionId: kotlin.String { get; }
/*∆*/ 
/*∆*/     public final fun close(): kotlin.js.Promise<kotlin.Unit>
/*∆*/ 
/*∆*/     public final fun generateRequest(initDataType: kotlin.String, initData: dynamic): kotlin.js.Promise<kotlin.Unit>
/*∆*/ 
/*∆*/     public final fun load(sessionId: kotlin.String): kotlin.js.Promise<kotlin.Boolean>
/*∆*/ 
/*∆*/     public final fun remove(): kotlin.js.Promise<kotlin.Unit>
/*∆*/ 
/*∆*/     public final fun update(response: dynamic): kotlin.js.Promise<kotlin.Unit>
/*∆*/ }
/*∆*/ 
/*∆*/ @kotlin.js.JsName(name = "null")
/*∆*/ public external interface MediaKeySessionType {
/*∆*/     public companion object of MediaKeySessionType {
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ @kotlin.js.JsName(name = "null")
/*∆*/ public external interface MediaKeyStatus {
/*∆*/     public companion object of MediaKeyStatus {
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class MediaKeyStatusMap {
/*∆*/     public constructor MediaKeyStatusMap()
/*∆*/ 
/*∆*/     public open val size: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public final fun get(keyId: dynamic): kotlin.Any?
/*∆*/ 
/*∆*/     public final fun has(keyId: dynamic): kotlin.Boolean
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class MediaKeySystemAccess {
/*∆*/     public constructor MediaKeySystemAccess()
/*∆*/ 
/*∆*/     public open val keySystem: kotlin.String { get; }
/*∆*/ 
/*∆*/     public final fun createMediaKeys(): kotlin.js.Promise<org.w3c.dom.encryptedmedia.MediaKeys>
/*∆*/ 
/*∆*/     public final fun getConfiguration(): org.w3c.dom.encryptedmedia.MediaKeySystemConfiguration
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface MediaKeySystemConfiguration {
/*∆*/     public open var audioCapabilities: kotlin.Array<org.w3c.dom.encryptedmedia.MediaKeySystemMediaCapability>? { get; set; }
/*∆*/ 
/*∆*/     public open var distinctiveIdentifier: org.w3c.dom.encryptedmedia.MediaKeysRequirement? { get; set; }
/*∆*/ 
/*∆*/     public open var initDataTypes: kotlin.Array<kotlin.String>? { get; set; }
/*∆*/ 
/*∆*/     public open var label: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var persistentState: org.w3c.dom.encryptedmedia.MediaKeysRequirement? { get; set; }
/*∆*/ 
/*∆*/     public open var sessionTypes: kotlin.Array<kotlin.String>? { get; set; }
/*∆*/ 
/*∆*/     public open var videoCapabilities: kotlin.Array<org.w3c.dom.encryptedmedia.MediaKeySystemMediaCapability>? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface MediaKeySystemMediaCapability {
/*∆*/     public open var contentType: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var robustness: kotlin.String? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class MediaKeys {
/*∆*/     public constructor MediaKeys()
/*∆*/ 
/*∆*/     public final fun createSession(sessionType: org.w3c.dom.encryptedmedia.MediaKeySessionType = ...): org.w3c.dom.encryptedmedia.MediaKeySession
/*∆*/ 
/*∆*/     public final fun setServerCertificate(serverCertificate: dynamic): kotlin.js.Promise<kotlin.Boolean>
/*∆*/ }
/*∆*/ 
/*∆*/ @kotlin.js.JsName(name = "null")
/*∆*/ public external interface MediaKeysRequirement {
/*∆*/     public companion object of MediaKeysRequirement {
/*∆*/     }
/*∆*/ }