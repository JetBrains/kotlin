function delay(timeMillis, $completion) {
  var tmp = new $delayCOROUTINE$0(timeMillis, $completion);
  tmp.result_1 = Unit_getInstance();
  tmp.exception_1 = null;
  return tmp.doResume_5yljmg_k$();
}