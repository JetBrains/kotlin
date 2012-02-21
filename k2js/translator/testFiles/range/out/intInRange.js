var classes = function(){
  return {};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    if ((new Kotlin.NumberRange(-2, 0 - -2 + 1, false)).contains(1))
      return false;
    if ((new Kotlin.NumberRange(-10, -4 - -10 + 1, false)).contains(1))
      return false;
    if (!(new Kotlin.NumberRange(0, 2 - 0 + 1, false)).contains(1))
      return false;
    if (!(new Kotlin.NumberRange(1, 2 - 1 + 1, false)).contains(1))
      return false;
    if (!(new Kotlin.NumberRange(-2, 5 - -2 + 1, false)).contains(1))
      return false;
    return true;
  }
}
, main:function(){
  {
    foo.box();
  }
}
}, {});
foo.initialize();
