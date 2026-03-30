function topLevel$ref() {
  var l = function () {
    topLevel();
    return Unit_instance;
  };
  l.callableName = 'topLevel';
  l.$flags = 0;
  l.$arity = 0;
  l.$id = 0;
  return l;
}