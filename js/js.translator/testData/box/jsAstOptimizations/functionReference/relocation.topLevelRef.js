function topLevel$ref() {
  return constructCallableReference(function () {
    topLevel();
    return Unit_instance;
  }, 0, 0, 0, 'topLevel');
}
