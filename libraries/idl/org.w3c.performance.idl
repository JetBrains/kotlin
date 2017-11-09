namespace org.w3c.performance;


// Downloaded from http://www.w3.org/TR/hr-time/
typedef double DOMHighResTimeStamp;
[Exposed=(Window,Worker)]
interface Performance : EventTarget {
 DOMHighResTimeStamp now();
 serializer = {attribute};
};
[NoInterfaceObject,
 Exposed=(Window,Worker)]
interface GlobalPerformance {
 [Replaceable] readonly attribute Performance performance;
};

Window implements GlobalPerformance;

WorkerGlobalScope implements GlobalPerformance;
// Downloaded from http://www.w3.org/TR/2012/REC-navigation-timing-20121217/
interface PerformanceTiming {
  readonly attribute unsigned long long navigationStart;
  readonly attribute unsigned long long unloadEventStart;
  readonly attribute unsigned long long unloadEventEnd;
  readonly attribute unsigned long long redirectStart;
  readonly attribute unsigned long long redirectEnd;
  readonly attribute unsigned long long fetchStart;
  readonly attribute unsigned long long domainLookupStart;
  readonly attribute unsigned long long domainLookupEnd;
  readonly attribute unsigned long long connectStart;
  readonly attribute unsigned long long connectEnd;
  readonly attribute unsigned long long secureConnectionStart;
  readonly attribute unsigned long long requestStart;
  readonly attribute unsigned long long responseStart;
  readonly attribute unsigned long long responseEnd;
  readonly attribute unsigned long long domLoading;
  readonly attribute unsigned long long domInteractive;
  readonly attribute unsigned long long domContentLoadedEventStart;
  readonly attribute unsigned long long domContentLoadedEventEnd;
  readonly attribute unsigned long long domComplete;
  readonly attribute unsigned long long loadEventStart;
  readonly attribute unsigned long long loadEventEnd;
};
interface PerformanceNavigation {
  const unsigned short TYPE_NAVIGATE = 0;
  const unsigned short TYPE_RELOAD = 1;
  const unsigned short TYPE_BACK_FORWARD = 2;
  const unsigned short TYPE_RESERVED = 255;
  readonly attribute unsigned short type;
  readonly attribute unsigned short redirectCount;
};
interface Performance {
  readonly attribute PerformanceTiming timing;
  readonly attribute PerformanceNavigation navigation;
};

partial interface Window {
  [Replaceable] readonly attribute Performance performance;
};

