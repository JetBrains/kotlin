var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    this.$c = 3;
  }
  , get_c:function(){
    return this.$c;
  }
  , set_c:function(tmp$0){
    this.$c = tmp$0;
  }
  });
  return {A:tmp$0};
}
();
var foo = Kotlin.Namespace.create({initialize:function(){
}
, i:function(receiver){
  {
    receiver.set_c(receiver.get_c() + 1);
    return receiver.get_c();
  }
}
, box:function(){
  {
    var tmp$7;
    var tmp$6;
    var tmp$5;
    var tmp$4;
    var tmp$3;
    var tmp$2;
    var tmp$1;
    var tmp$0;
    var a1 = new foo.A;
    var a2 = null;
    if ((tmp$0 = a1 , tmp$0 != null?foo.i(tmp$0):null) != 4) {
      return '1';
    }
    if ((tmp$1 = a1 , tmp$1 != null?tmp$1.get_c():null) != 4) {
      return '2';
    }
    if ((tmp$2 = a2 , tmp$2 != null?tmp$2.get_c():null) != null) {
      return '3';
    }
    tmp$3 = a2 , tmp$3 != null?foo.i(tmp$3):null;
    if ((tmp$4 = a1 , tmp$4 != null?tmp$4.get_c():null) != 4) {
      return '4';
    }
    a2 = a1;
    if ((tmp$5 = a2 , tmp$5 != null?foo.i(tmp$5):null) != 5) {
      return '5';
    }
    if ((tmp$6 = a2 , tmp$6 != null?foo.i(tmp$6):null) != 6 || (tmp$7 = a1 , tmp$7 != null?tmp$7.get_c():null) != 6) {
      return '6';
    }
    return 'OK';
  }
}
}, {A:classes.A});
foo.initialize();
