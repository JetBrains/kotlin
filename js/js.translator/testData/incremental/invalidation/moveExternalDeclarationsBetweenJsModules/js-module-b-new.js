(function (_) {
  'use strict';
  function externalDemoFunction() { return 2; }

  _.externalDemoFunction = externalDemoFunction;

  return _
}(module.exports));
