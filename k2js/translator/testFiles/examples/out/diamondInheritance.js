var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    this.$v = 0;
  }
  , get_v:function(){
    return this.$v;
  }
  , set_v:function(tmp$0){
    this.$v = tmp$0;
  }
  });
  var tmp$1 = Kotlin.Trait.create(tmp$0, {});
  var tmp$2 = Kotlin.Class.create(tmp$0, {initialize:function(){
    this.super_init();
  }
  });
  var tmp$3 = Kotlin.Class.create(tmp$2, tmp$1, {initialize:function(){
    this.super_init();
  }
  });
  return {Right:tmp$1, Left:tmp$2, D:tmp$3, Base:tmp$0};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, vl:function(l){
  {
    return l.get_v();
  }
}
, vr:function(r){
  {
    return r.get_v();
  }
}
, box:function(){
  {
    var d = new Anonymous.D;
    d.set_v(42);
    if (d.get_v() != 42)
      return 'Fail #1';
    if (Anonymous.vl(d) != 42)
      return 'Fail #2';
    if (Anonymous.vr(d) != 42)
      return 'Fail #3';
    return 'OK';
  }
}
}, {Base:classes.Base, Left:classes.Left, Right:classes.Right, D:classes.D});
Anonymous.initialize();
