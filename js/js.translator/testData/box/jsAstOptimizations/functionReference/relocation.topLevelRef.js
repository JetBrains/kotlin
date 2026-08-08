function topLevel$ref() {
  return constructCallableReference(function () {
    topLevel();
    return Unit$instance;
  }, 0, 0, 0, 'topLevel');
}
