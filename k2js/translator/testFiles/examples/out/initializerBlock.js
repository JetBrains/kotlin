var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    {
      this.$f = 610;
    }
  }
  , get_f:function(){
    return this.$f;
  }
  , set_f:function(tmp$0){
    this.$f = tmp$0;
  }
  });
  return {C:tmp$0};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var c = new Anonymous.C;
    if (c.get_f() != 610)
      return 'fail';
    return 'OK';
  }
}
}, {C:classes.C});
Anonymous.initialize();
