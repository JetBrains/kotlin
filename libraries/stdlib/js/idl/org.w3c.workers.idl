namespace org.w3c.workers;


// Downloaded from https://w3c.github.io/ServiceWorker/
[SecureContext, Exposed=(Window,Worker)]
interface ServiceWorker : EventTarget {
  readonly attribute USVString scriptURL;
  readonly attribute ServiceWorkerState state;
  void postMessage(any message, optional sequence<object> transfer);

  // event
  attribute EventHandler onstatechange;
};
ServiceWorker implements AbstractWorker;

enum ServiceWorkerState {
  "installing",
  "installed",
  "activating",
  "activated",
  "redundant"
};
[SecureContext, Exposed=(Window,Worker)]
interface ServiceWorkerRegistration : EventTarget {
  readonly attribute ServiceWorker? installing;
  readonly attribute ServiceWorker? waiting;
  readonly attribute ServiceWorker? active;

  readonly attribute USVString scope;

  [NewObject] Promise<void> update();
  [NewObject] Promise<boolean> unregister();

  // event
  attribute EventHandler onupdatefound;
};
partial interface Navigator {
  [SecureContext, SameObject] readonly attribute ServiceWorkerContainer serviceWorker;
};

partial interface WorkerNavigator {
  [SecureContext, SameObject] readonly attribute ServiceWorkerContainer serviceWorker;
};
[SecureContext, Exposed=(Window,Worker)]
interface ServiceWorkerContainer : EventTarget {
  readonly attribute ServiceWorker? controller;
  [SameObject] readonly attribute Promise<ServiceWorkerRegistration> ready;

  [NewObject] Promise<ServiceWorkerRegistration> register(USVString scriptURL, optional RegistrationOptions options);

  [NewObject] Promise<any> getRegistration(optional USVString clientURL = "");
  [NewObject] Promise<sequence<ServiceWorkerRegistration>> getRegistrations();

  void startMessages();


  // events
  attribute EventHandler oncontrollerchange;
  attribute EventHandler onmessage; // event.source of message events is ServiceWorker object
};
dictionary RegistrationOptions {
  USVString scope;
  WorkerType type = "classic";
};
[Constructor(DOMString type, optional ServiceWorkerMessageEventInit eventInitDict), Exposed=(Window,Worker)]
interface ServiceWorkerMessageEvent : Event {
  readonly attribute any data;
  readonly attribute DOMString origin;
  readonly attribute DOMString lastEventId;
  [SameObject] readonly attribute (ServiceWorker or MessagePort)? source;
  readonly attribute FrozenArray<MessagePort>? ports;
};
dictionary ServiceWorkerMessageEventInit : EventInit {
  any data;
  DOMString origin;
  DOMString lastEventId;
  (ServiceWorker or MessagePort)? source;
  sequence<MessagePort>? ports;
};
[Global=(Worker,ServiceWorker), Exposed=ServiceWorker]
interface ServiceWorkerGlobalScope : WorkerGlobalScope {
  // A container for a list of Client objects that correspond to
  // browsing contexts (or shared workers) that are on the origin of this SW
  [SameObject] readonly attribute Clients clients;
  [SameObject] readonly attribute ServiceWorkerRegistration registration;

  [NewObject] Promise<void> skipWaiting();

  attribute EventHandler oninstall;
  attribute EventHandler onactivate;
  attribute EventHandler onfetch;
  attribute EventHandler onforeignfetch;

  // event
  attribute EventHandler onmessage; // event.source of the message events is Client object
};
[Exposed=ServiceWorker]
interface Client {
  readonly attribute USVString url;
  readonly attribute FrameType frameType;
  readonly attribute DOMString id;
  void postMessage(any message, optional sequence<object> transfer);
};

[Exposed=ServiceWorker]
interface WindowClient : Client {
  readonly attribute VisibilityState visibilityState;
  readonly attribute boolean focused;
  [NewObject] Promise<WindowClient> focus();
  [NewObject] Promise<WindowClient> navigate(USVString url);
};

enum FrameType {
  "auxiliary",
  "top-level",
  "nested",
  "none"
};
[Exposed=ServiceWorker]
interface Clients {
  // The objects returned will be new instances every time
  [NewObject] Promise<any> get(DOMString id);
  [NewObject] Promise<sequence<Client>> matchAll(optional ClientQueryOptions options);
  [NewObject] Promise<WindowClient?> openWindow(USVString url);
  [NewObject] Promise<void> claim();
};
dictionary ClientQueryOptions {
  boolean includeUncontrolled = false;
  ClientType type = "window";
};
enum ClientType {
  "window",
  "worker",
  "sharedworker",
  "all"
};
[Constructor(DOMString type, optional ExtendableEventInit eventInitDict), Exposed=ServiceWorker]
interface ExtendableEvent : Event {
  void waitUntil(Promise<any> f);
};
dictionary ExtendableEventInit : EventInit {
  // Defined for the forward compatibility across the derived events
};
[Constructor(DOMString type, optional ExtendableEventInit eventInitDict), Exposed=ServiceWorker]
interface InstallEvent : ExtendableEvent {
  void registerForeignFetch(ForeignFetchOptions options);
};

dictionary ForeignFetchOptions {
  required sequence<USVString> scopes;
  required sequence<USVString> origins;
};
[Constructor(DOMString type, FetchEventInit eventInitDict), Exposed=ServiceWorker]
interface FetchEvent : ExtendableEvent {
  [SameObject] readonly attribute Request request;
  readonly attribute DOMString? clientId;
  readonly attribute boolean isReload;

  void respondWith(Promise<Response> r);
};
dictionary FetchEventInit : ExtendableEventInit {
  required Request request;
  DOMString? clientId = null;
  boolean isReload = false;
};
[Constructor(DOMString type, ForeignFetchEventInit eventInitDict), Exposed=ServiceWorker]
interface ForeignFetchEvent : ExtendableEvent {
  [SameObject] readonly attribute Request request;
  readonly attribute USVString origin;

  void respondWith(Promise<ForeignFetchResponse> r);
};

dictionary ForeignFetchEventInit : ExtendableEventInit {
  required Request request;
  USVString origin = "null";
};

dictionary ForeignFetchResponse {
  required Response response;
  USVString origin;
  sequence<ByteString> headers;
};
[Constructor(DOMString type, optional ExtendableMessageEventInit eventInitDict), Exposed=ServiceWorker]
interface ExtendableMessageEvent : ExtendableEvent {
  readonly attribute any data;
  readonly attribute DOMString origin;
  readonly attribute DOMString lastEventId;
  [SameObject] readonly attribute (Client or ServiceWorker or MessagePort)? source;
  readonly attribute FrozenArray<MessagePort>? ports;
};
dictionary ExtendableMessageEventInit : ExtendableEventInit {
  any data;
  DOMString origin;
  DOMString lastEventId;
  (Client or ServiceWorker or MessagePort)? source;
  sequence<MessagePort>? ports;
};
partial interface HTMLLinkElement {
  [CEReactions] attribute USVString scope;
  [CEReactions] attribute WorkerType workerType;
};
partial interface WindowOrWorkerGlobalScope {
  [SecureContext, SameObject] readonly attribute CacheStorage caches;
};
[SecureContext, Exposed=(Window,Worker)]
interface Cache {
  [NewObject] Promise<any> match(RequestInfo request, optional CacheQueryOptions options);
  [NewObject] Promise<sequence<Response>> matchAll(optional RequestInfo request, optional CacheQueryOptions options);
  [NewObject] Promise<void> add(RequestInfo request);
  [NewObject] Promise<void> addAll(sequence<RequestInfo> requests);
  [NewObject] Promise<void> put(RequestInfo request, Response response);
  [NewObject] Promise<boolean> delete(RequestInfo request, optional CacheQueryOptions options);
  [NewObject] Promise<sequence<Request>> keys(optional RequestInfo request, optional CacheQueryOptions options);
};
dictionary CacheQueryOptions {
  boolean ignoreSearch = false;
  boolean ignoreMethod = false;
  boolean ignoreVary = false;
  DOMString cacheName;
};
dictionary CacheBatchOperation {
  DOMString type;
  Request request;
  Response response;
  CacheQueryOptions options;
};
[SecureContext, Exposed=(Window,Worker)]
interface CacheStorage {
  [NewObject] Promise<any> match(RequestInfo request, optional CacheQueryOptions options);
  [NewObject] Promise<boolean> has(DOMString cacheName);
  [NewObject] Promise<Cache> open(DOMString cacheName);
  [NewObject] Promise<boolean> delete(DOMString cacheName);
  [NewObject] Promise<sequence<DOMString>> keys();
};
partial interface ServiceWorkerRegistration {
  // e.g. define an API namespace
  readonly attribute APISpaceType APISpace;
  // e.g. define a method
  Promise<T> methodName(/* list of arguments */);
};
// e.g. define FunctionalEvent interface
interface FunctionalEvent : ExtendableEvent {
  // add a functional event’s own attributes and methods
};
partial interface ServiceWorkerGlobalScope {
  attribute EventHandler onfunctionalevent;
};
[SecureContext, Exposed=(Window,Worker)]
interface ServiceWorker : EventTarget {
  readonly attribute USVString scriptURL;
  readonly attribute ServiceWorkerState state;
  void postMessage(any message, optional sequence<object> transfer);

  // event
  attribute EventHandler onstatechange;
};
ServiceWorker implements AbstractWorker;

enum ServiceWorkerState {
  "installing",
  "installed",
  "activating",
  "activated",
  "redundant"
};

[SecureContext, Exposed=(Window,Worker)]
interface ServiceWorkerRegistration : EventTarget {
  readonly attribute ServiceWorker? installing;
  readonly attribute ServiceWorker? waiting;
  readonly attribute ServiceWorker? active;

  readonly attribute USVString scope;

  [NewObject] Promise<void> update();
  [NewObject] Promise<boolean> unregister();

  // event
  attribute EventHandler onupdatefound;
};

partial interface Navigator {
  [SecureContext, SameObject] readonly attribute ServiceWorkerContainer serviceWorker;
};

partial interface WorkerNavigator {
  [SecureContext, SameObject] readonly attribute ServiceWorkerContainer serviceWorker;
};

[SecureContext, Exposed=(Window,Worker)]
interface ServiceWorkerContainer : EventTarget {
  readonly attribute ServiceWorker? controller;
  [SameObject] readonly attribute Promise<ServiceWorkerRegistration> ready;

  [NewObject] Promise<ServiceWorkerRegistration> register(USVString scriptURL, optional RegistrationOptions options);

  [NewObject] Promise<any> getRegistration(optional USVString clientURL = "");
  [NewObject] Promise<sequence<ServiceWorkerRegistration>> getRegistrations();

  void startMessages();


  // events
  attribute EventHandler oncontrollerchange;
  attribute EventHandler onmessage; // event.source of message events is ServiceWorker object
};

dictionary RegistrationOptions {
  USVString scope;
  WorkerType type = "classic";
};

[Constructor(DOMString type, optional ServiceWorkerMessageEventInit eventInitDict), Exposed=(Window,Worker)]
interface ServiceWorkerMessageEvent : Event {
  readonly attribute any data;
  readonly attribute DOMString origin;
  readonly attribute DOMString lastEventId;
  [SameObject] readonly attribute (ServiceWorker or MessagePort)? source;
  readonly attribute FrozenArray<MessagePort>? ports;
};

dictionary ServiceWorkerMessageEventInit : EventInit {
  any data;
  DOMString origin;
  DOMString lastEventId;
  (ServiceWorker or MessagePort)? source;
  sequence<MessagePort>? ports;
};

[Global=(Worker,ServiceWorker), Exposed=ServiceWorker]
interface ServiceWorkerGlobalScope : WorkerGlobalScope {
  // A container for a list of Client objects that correspond to
  // browsing contexts (or shared workers) that are on the origin of this SW
  [SameObject] readonly attribute Clients clients;
  [SameObject] readonly attribute ServiceWorkerRegistration registration;

  [NewObject] Promise<void> skipWaiting();

  attribute EventHandler oninstall;
  attribute EventHandler onactivate;
  attribute EventHandler onfetch;
  attribute EventHandler onforeignfetch;

  // event
  attribute EventHandler onmessage; // event.source of the message events is Client object
};

[Exposed=ServiceWorker]
interface Client {
  readonly attribute USVString url;
  readonly attribute FrameType frameType;
  readonly attribute DOMString id;
  void postMessage(any message, optional sequence<object> transfer);
};

[Exposed=ServiceWorker]
interface WindowClient : Client {
  readonly attribute VisibilityState visibilityState;
  readonly attribute boolean focused;
  [NewObject] Promise<WindowClient> focus();
  [NewObject] Promise<WindowClient> navigate(USVString url);
};

enum FrameType {
  "auxiliary",
  "top-level",
  "nested",
  "none"
};

[Exposed=ServiceWorker]
interface Clients {
  // The objects returned will be new instances every time
  [NewObject] Promise<any> get(DOMString id);
  [NewObject] Promise<sequence<Client>> matchAll(optional ClientQueryOptions options);
  [NewObject] Promise<WindowClient?> openWindow(USVString url);
  [NewObject] Promise<void> claim();
};

dictionary ClientQueryOptions {
  boolean includeUncontrolled = false;
  ClientType type = "window";
};

enum ClientType {
  "window",
  "worker",
  "sharedworker",
  "all"
};

[Constructor(DOMString type, optional ExtendableEventInit eventInitDict), Exposed=ServiceWorker]
interface ExtendableEvent : Event {
  void waitUntil(Promise<any> f);
};

dictionary ExtendableEventInit : EventInit {
  // Defined for the forward compatibility across the derived events
};

[Constructor(DOMString type, optional ExtendableEventInit eventInitDict), Exposed=ServiceWorker]
interface InstallEvent : ExtendableEvent {
  void registerForeignFetch(ForeignFetchOptions options);
};

dictionary ForeignFetchOptions {
  required sequence<USVString> scopes;
  required sequence<USVString> origins;
};

[Constructor(DOMString type, FetchEventInit eventInitDict), Exposed=ServiceWorker]
interface FetchEvent : ExtendableEvent {
  [SameObject] readonly attribute Request request;
  readonly attribute DOMString? clientId;
  readonly attribute boolean isReload;

  void respondWith(Promise<Response> r);
};

dictionary FetchEventInit : ExtendableEventInit {
  required Request request;
  DOMString? clientId = null;
  boolean isReload = false;
};

[Constructor(DOMString type, ForeignFetchEventInit eventInitDict), Exposed=ServiceWorker]
interface ForeignFetchEvent : ExtendableEvent {
  [SameObject] readonly attribute Request request;
  readonly attribute USVString origin;

  void respondWith(Promise<ForeignFetchResponse> r);
};

dictionary ForeignFetchEventInit : ExtendableEventInit {
  required Request request;
  USVString origin = "null";
};

dictionary ForeignFetchResponse {
  required Response response;
  USVString origin;
  sequence<ByteString> headers;
};

[Constructor(DOMString type, optional ExtendableMessageEventInit eventInitDict), Exposed=ServiceWorker]
interface ExtendableMessageEvent : ExtendableEvent {
  readonly attribute any data;
  readonly attribute DOMString origin;
  readonly attribute DOMString lastEventId;
  [SameObject] readonly attribute (Client or ServiceWorker or MessagePort)? source;
  readonly attribute FrozenArray<MessagePort>? ports;
};

dictionary ExtendableMessageEventInit : ExtendableEventInit {
  any data;
  DOMString origin;
  DOMString lastEventId;
  (Client or ServiceWorker or MessagePort)? source;
  sequence<MessagePort>? ports;
};

partial interface HTMLLinkElement {
  [CEReactions] attribute USVString scope;
  [CEReactions] attribute WorkerType workerType;
};

partial interface WindowOrWorkerGlobalScope {
  [SecureContext, SameObject] readonly attribute CacheStorage caches;
};

[SecureContext, Exposed=(Window,Worker)]
interface Cache {
  [NewObject] Promise<any> match(RequestInfo request, optional CacheQueryOptions options);
  [NewObject] Promise<sequence<Response>> matchAll(optional RequestInfo request, optional CacheQueryOptions options);
  [NewObject] Promise<void> add(RequestInfo request);
  [NewObject] Promise<void> addAll(sequence<RequestInfo> requests);
  [NewObject] Promise<void> put(RequestInfo request, Response response);
  [NewObject] Promise<boolean> delete(RequestInfo request, optional CacheQueryOptions options);
  [NewObject] Promise<sequence<Request>> keys(optional RequestInfo request, optional CacheQueryOptions options);
};

dictionary CacheQueryOptions {
  boolean ignoreSearch = false;
  boolean ignoreMethod = false;
  boolean ignoreVary = false;
  DOMString cacheName;
};

dictionary CacheBatchOperation {
  DOMString type;
  Request request;
  Response response;
  CacheQueryOptions options;
};

[SecureContext, Exposed=(Window,Worker)]
interface CacheStorage {
  [NewObject] Promise<any> match(RequestInfo request, optional CacheQueryOptions options);
  [NewObject] Promise<boolean> has(DOMString cacheName);
  [NewObject] Promise<Cache> open(DOMString cacheName);
  [NewObject] Promise<boolean> delete(DOMString cacheName);
  [NewObject] Promise<sequence<DOMString>> keys();
};

