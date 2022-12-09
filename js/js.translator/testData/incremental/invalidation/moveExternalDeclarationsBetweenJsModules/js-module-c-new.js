(function (_) {
  'use strict';
  function externalDemoFunction() { return 5; }

  _.externalDemoFunction = externalDemoFunction;

  return _
}(module.exports));
