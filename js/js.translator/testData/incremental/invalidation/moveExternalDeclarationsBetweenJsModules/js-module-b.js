(function (_) {
  'use strict';
  function externalDemoFunction() { return 1; }

  _.externalDemoFunction = externalDemoFunction;

  return _
}(module.exports));
