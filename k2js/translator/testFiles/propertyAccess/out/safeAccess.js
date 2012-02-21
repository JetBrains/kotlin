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
, box:function(){
  {
    var tmp$5;
    var tmp$4;
    var tmp$3;
    var tmp$2;
    var tmp$1;
    var tmp$0;
    var a1 = new foo.A;
    var a2 = null;
    tmp$0 = a1 , tmp$0 != null?tmp$0.set_c(4):null;
    tmp$1 = a2 , tmp$1 != null?tmp$1.set_c(5):null;
    if ((tmp$2 = a1 , tmp$2 != null?tmp$2.get_c():null) != 4) {
      return false;
    }
    a2 = a1;
    tmp$3 = a2 , tmp$3 != null?tmp$3.set_c(5):null;
    return (tmp$4 = a2 , tmp$4 != null?tmp$4.get_c():null) == 5 && (tmp$5 = a1 , tmp$5 != null?tmp$5.get_c():null) == 5;
  }
}
}, {A:classes.A});
foo.initialize();
