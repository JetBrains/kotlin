namespace org.w3c.notifications;


// Downloaded from https://raw.githubusercontent.com/whatwg/notifications/master/notifications.html
[Constructor(DOMString title, optional NotificationOptions options),
 Exposed=(Window,Worker)]
interface Notification : EventTarget {
  static readonly attribute NotificationPermission permission;
  [Exposed=Window] static Promise<NotificationPermission> requestPermission(optional NotificationPermissionCallback deprecatedCallback);

  static readonly attribute unsigned long maxActions;

  attribute EventHandler onclick;
  attribute EventHandler onerror;

  readonly attribute DOMString title;
  readonly attribute NotificationDirection dir;
  readonly attribute DOMString lang;
  readonly attribute DOMString body;
  readonly attribute DOMString tag;
  readonly attribute USVString image;
  readonly attribute USVString icon;
  readonly attribute USVString badge;
  readonly attribute USVString sound;
  [SameObject] readonly attribute FrozenArray<unsigned long> vibrate;
  readonly attribute DOMTimeStamp timestamp;
  readonly attribute boolean renotify;
  readonly attribute boolean silent;
  readonly attribute boolean noscreen;
  readonly attribute boolean requireInteraction;
  readonly attribute boolean sticky;
  [SameObject] readonly attribute any data;
  [SameObject] readonly attribute FrozenArray<NotificationAction> actions;

  void close();
};

dictionary NotificationOptions {
  NotificationDirection dir = "auto";
  DOMString lang = "";
  DOMString body = "";
  DOMString tag = "";
  USVString image;
  USVString icon;
  USVString badge;
  USVString sound;
  VibratePattern vibrate;
  DOMTimeStamp timestamp;
  boolean renotify = false;
  boolean silent = false;
  boolean noscreen = false;
  boolean requireInteraction = false;
  boolean sticky = false;
  any data = null;
  sequence<NotificationAction> actions = [];
};

enum NotificationPermission {
  "default",
  "denied",
  "granted"
};

enum NotificationDirection {
  "auto",
  "ltr",
  "rtl"
};

dictionary NotificationAction {
  required DOMString action;
  required DOMString title;
  USVString icon;
};

callback NotificationPermissionCallback = void (NotificationPermission permission);
dictionary GetNotificationOptions {
  DOMString tag = "";
};

partial interface ServiceWorkerRegistration {
  Promise<void> showNotification(DOMString title, optional NotificationOptions options);
  Promise<sequence<Notification>> getNotifications(optional GetNotificationOptions filter);
};

[Constructor(DOMString type, NotificationEventInit eventInitDict),
 Exposed=ServiceWorker]
interface NotificationEvent : ExtendableEvent {
  readonly attribute Notification notification;
  readonly attribute DOMString action;
};

dictionary NotificationEventInit : ExtendableEventInit {
  required Notification notification;
  DOMString action = "";
};

partial interface ServiceWorkerGlobalScope {
  attribute EventHandler onnotificationclick;
  attribute EventHandler onnotificationclose;
};
[Constructor(DOMString title, optional NotificationOptions options),
 Exposed=(Window,Worker)]
interface Notification : EventTarget {
  static readonly attribute NotificationPermission permission;
  [Exposed=Window] static Promise<NotificationPermission> requestPermission(optional NotificationPermissionCallback deprecatedCallback);

  static readonly attribute unsigned long maxActions;

  attribute EventHandler onclick;
  attribute EventHandler onerror;

  readonly attribute DOMString title;
  readonly attribute NotificationDirection dir;
  readonly attribute DOMString lang;
  readonly attribute DOMString body;
  readonly attribute DOMString tag;
  readonly attribute USVString image;
  readonly attribute USVString icon;
  readonly attribute USVString badge;
  readonly attribute USVString sound;
  [SameObject] readonly attribute FrozenArray<unsigned long> vibrate;
  readonly attribute DOMTimeStamp timestamp;
  readonly attribute boolean renotify;
  readonly attribute boolean silent;
  readonly attribute boolean noscreen;
  readonly attribute boolean requireInteraction;
  readonly attribute boolean sticky;
  [SameObject] readonly attribute any data;
  [SameObject] readonly attribute FrozenArray<NotificationAction> actions;

  void close();
};

dictionary NotificationOptions {
  NotificationDirection dir = "auto";
  DOMString lang = "";
  DOMString body = "";
  DOMString tag = "";
  USVString image;
  USVString icon;
  USVString badge;
  USVString sound;
  VibratePattern vibrate;
  DOMTimeStamp timestamp;
  boolean renotify = false;
  boolean silent = false;
  boolean noscreen = false;
  boolean requireInteraction = false;
  boolean sticky = false;
  any data = null;
  sequence<NotificationAction> actions = [];
};

enum NotificationPermission {
  "default",
  "denied",
  "granted"
};

enum NotificationDirection {
  "auto",
  "ltr",
  "rtl"
};

dictionary NotificationAction {
  required DOMString action;
  required DOMString title;
  USVString icon;
};

callback NotificationPermissionCallback = void (NotificationPermission permission);

dictionary GetNotificationOptions {
  DOMString tag = "";
};

partial interface ServiceWorkerRegistration {
  Promise<void> showNotification(DOMString title, optional NotificationOptions options);
  Promise<sequence<Notification>> getNotifications(optional GetNotificationOptions filter);
};

[Constructor(DOMString type, NotificationEventInit eventInitDict),
 Exposed=ServiceWorker]
interface NotificationEvent : ExtendableEvent {
  readonly attribute Notification notification;
  readonly attribute DOMString action;
};

dictionary NotificationEventInit : ExtendableEventInit {
  required Notification notification;
  DOMString action = "";
};

partial interface ServiceWorkerGlobalScope {
  attribute EventHandler onnotificationclick;
  attribute EventHandler onnotificationclose;
};

