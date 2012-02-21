var classes = function(){
  var tmp$0 = Kotlin.Class.create({initialize:function(){
    this.$contents = new Kotlin.ArrayList;
  }
  , get_contents:function(){
    return this.$contents;
  }
  , add:function(item){
    {
      this.get_contents().add(item);
    }
  }
  , plus:function(b){
    {
      var result = new Anonymous.ArrayWrapper;
      result.get_contents().addAll(this.get_contents());
      result.get_contents().addAll(b.get_contents());
      return result;
    }
  }
  });
  return {ArrayWrapper:tmp$0};
}
();
var Anonymous = Kotlin.Namespace.create({initialize:function(){
}
, box:function(){
  {
    var tmp$0;
    var v1 = new Anonymous.ArrayWrapper;
    var v2 = new Anonymous.ArrayWrapper;
    v1.add('foo');
    v2.add('bar');
    var v3 = v1.plus(v2);
    if (v3.get_contents().size() == 2)
      tmp$0 = 'OK';
    else 
      tmp$0 = 'fail';
    return tmp$0;
  }
}
}, {ArrayWrapper:classes.ArrayWrapper});
Anonymous.initialize();
