function topLevel$ref() {
  return constructCallableReference(() => {
    topLevel();
    return Unit$instance;
  }, 0, 0, 0, 'topLevel');
}
