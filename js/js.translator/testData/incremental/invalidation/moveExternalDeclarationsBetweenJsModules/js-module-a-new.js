(function (_) {
  'use strict';
  function externalDemoFunction() { return 3; }

  _.externalDemoFunction = externalDemoFunction;

  return _
}(module.exports));
