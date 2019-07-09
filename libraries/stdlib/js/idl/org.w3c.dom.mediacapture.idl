namespace org.w3c.dom.mediacapture;


// Downloaded from https://w3c.github.io/mediacapture-main/
[Exposed=Window,
 Constructor,
 Constructor (MediaStream stream),
 Constructor (sequence<MediaStreamTrack> tracks)]
interface MediaStream : EventTarget {
    readonly        attribute DOMString    id;
    sequence<MediaStreamTrack> getAudioTracks ();
    sequence<MediaStreamTrack> getVideoTracks ();
    sequence<MediaStreamTrack> getTracks ();
    MediaStreamTrack?          getTrackById (DOMString trackId);
    void                       addTrack (MediaStreamTrack track);
    void                       removeTrack (MediaStreamTrack track);
    MediaStream                clone ();
    readonly        attribute boolean      active;
                    attribute EventHandler onaddtrack;
                    attribute EventHandler onremovetrack;
};
[Exposed=Window]
interface MediaStreamTrack : EventTarget {
    readonly        attribute DOMString             kind;
    readonly        attribute DOMString             id;
    readonly        attribute DOMString             label;
                    attribute boolean               enabled;
    readonly        attribute boolean               muted;
                    attribute EventHandler          onmute;
                    attribute EventHandler          onunmute;
    readonly        attribute MediaStreamTrackState readyState;
                    attribute EventHandler          onended;
    MediaStreamTrack       clone ();
    void                   stop ();
    MediaTrackCapabilities getCapabilities ();
    MediaTrackConstraints  getConstraints ();
    MediaTrackSettings     getSettings ();
    Promise<void>          applyConstraints (optional MediaTrackConstraints constraints);
                    attribute EventHandler          onoverconstrained;
};
enum MediaStreamTrackState {
    "live",
    "ended"
};
dictionary MediaTrackSupportedConstraints {
             boolean width = true;
             boolean height = true;
             boolean aspectRatio = true;
             boolean frameRate = true;
             boolean facingMode = true;
             boolean resizeMode = true;
             boolean volume = true;
             boolean sampleRate = true;
             boolean sampleSize = true;
             boolean echoCancellation = true;
             boolean autoGainControl = true;
             boolean noiseSuppression = true;
             boolean latency = true;
             boolean channelCount = true;
             boolean deviceId = true;
             boolean groupId = true;
};
dictionary MediaTrackCapabilities {
             ULongRange           width;
             ULongRange           height;
             DoubleRange         aspectRatio;
             DoubleRange         frameRate;
             sequence<DOMString> facingMode;
             sequence<DOMString> resizeMode;
             DoubleRange         volume;
             ULongRange           sampleRate;
             ULongRange           sampleSize;
             sequence<boolean>   echoCancellation;
             sequence<boolean>   autoGainControl;
             sequence<boolean>   noiseSuppression;
             DoubleRange         latency;
             ULongRange           channelCount;
             DOMString           deviceId;
             DOMString           groupId;
};
dictionary MediaTrackConstraints : MediaTrackConstraintSet {
             sequence<MediaTrackConstraintSet> advanced;
};
dictionary MediaTrackConstraintSet {
             ConstrainULong      width;
             ConstrainULong      height;
             ConstrainDouble    aspectRatio;
             ConstrainDouble    frameRate;
             ConstrainDOMString facingMode;
             ConstrainDOMString resizeMode;
             ConstrainDouble    volume;
             ConstrainULong      sampleRate;
             ConstrainULong      sampleSize;
             ConstrainBoolean   echoCancellation;
             ConstrainBoolean   autoGainControl;
             ConstrainBoolean   noiseSuppression;
             ConstrainDouble    latency;
             ConstrainULong      channelCount;
             ConstrainDOMString deviceId;
             ConstrainDOMString groupId;
};
dictionary MediaTrackSettings {
             long      width;
             long      height;
             double    aspectRatio;
             double    frameRate;
             DOMString facingMode;
             DOMString resizeMode;
             double    volume;
             long      sampleRate;
             long      sampleSize;
             boolean   echoCancellation;
             boolean   autoGainControl;
             boolean   noiseSuppression;
             double    latency;
             long      channelCount;
             DOMString deviceId;
             DOMString groupId;
};
enum VideoFacingModeEnum {
    "user",
    "environment",
    "left",
    "right"
};
enum VideoResizeModeEnum {
    "none",
    "crop-and-scale"
};
[Exposed=Window,
 Constructor (DOMString type, MediaStreamTrackEventInit eventInitDict)]
interface MediaStreamTrackEvent : Event {
    [SameObject]
    readonly        attribute MediaStreamTrack track;
};
dictionary MediaStreamTrackEventInit : EventInit {
    required MediaStreamTrack track;
};
[Exposed=Window,
 Constructor (DOMString type, OverconstrainedErrorEventInit eventInitDict)]
interface OverconstrainedErrorEvent : Event {
    readonly        attribute OverconstrainedError? error;
};
dictionary OverconstrainedErrorEventInit : EventInit {
             OverconstrainedError? error = null;
};
partial interface Navigator {
    [SameObject, SecureContext]
    readonly        attribute MediaDevices mediaDevices;
};
[Exposed=Window,
SecureContext]
interface MediaDevices : EventTarget {
                    attribute EventHandler ondevicechange;
    Promise<sequence<MediaDeviceInfo>> enumerateDevices ();
};
[Exposed=Window]
interface MediaDeviceInfo {
    readonly        attribute DOMString       deviceId;
    readonly        attribute MediaDeviceKind kind;
    readonly        attribute DOMString       label;
    readonly        attribute DOMString       groupId;
    [Default] object toJSON();
};
enum MediaDeviceKind {
    "audioinput",
    "audiooutput",
    "videoinput"
};
[Exposed=Window] interface InputDeviceInfo : MediaDeviceInfo {
    MediaTrackCapabilities getCapabilities ();
};
partial interface Navigator {
    [SecureContext]
    void getUserMedia (MediaStreamConstraints constraints, NavigatorUserMediaSuccessCallback successCallback, NavigatorUserMediaErrorCallback errorCallback);
};
partial interface MediaDevices {
    MediaTrackSupportedConstraints getSupportedConstraints ();
    Promise<MediaStream>           getUserMedia (optional MediaStreamConstraints constraints);
};
dictionary MediaStreamConstraints {
             (boolean or MediaTrackConstraints) video = false;
             (boolean or MediaTrackConstraints) audio = false;
};
callback NavigatorUserMediaSuccessCallback = void (MediaStream stream);
callback NavigatorUserMediaErrorCallback = void (MediaStreamError error);
typedef object MediaStreamError;
[NoInterfaceObject]
interface ConstrainablePattern {
    Capabilities  getCapabilities ();
    Constraints   getConstraints ();
    Settings      getSettings ();
    Promise<void> applyConstraints (optional Constraints constraints);
                    attribute EventHandler onoverconstrained;
};
dictionary DoubleRange {
             double max;
             double min;
};
dictionary ConstrainDoubleRange : DoubleRange {
             double exact;
             double ideal;
};
dictionary ULongRange {
             [Clamp] unsigned long max;
             [Clamp] unsigned long min;
};
dictionary ConstrainULongRange : ULongRange {
             [Clamp] unsigned long exact;
             [Clamp] unsigned long ideal;
};
dictionary ConstrainBooleanParameters {
             boolean exact;
             boolean ideal;
};
dictionary ConstrainDOMStringParameters {
             (DOMString or sequence<DOMString>) exact;
             (DOMString or sequence<DOMString>) ideal;
};
typedef ([Clamp] unsigned long or ConstrainULongRange) ConstrainULong;
typedef (double or ConstrainDoubleRange) ConstrainDouble;
typedef (boolean or ConstrainBooleanParameters) ConstrainBoolean;
typedef (DOMString or sequence<DOMString> or ConstrainDOMStringParameters) ConstrainDOMString;
dictionary Capabilities {
};
dictionary Settings {
};
dictionary ConstraintSet {
};
dictionary Constraints : ConstraintSet {
             sequence<ConstraintSet> advanced;
};

