function topLevel$ref() {
  return constructCallableReference(() => {
    topLevel();
    return Unit_instance;
  }, 0, 0, 0, 'topLevel');
}
