{
  classes = function(){
    return {};
  }
  ();
  foo = Namespace.create(classes, {initialize:function(){
    this.$b = 0
  }
  , set_b:function(tmp$0){
    this.$b = tmp$0;
  }
  , get_b:function(){
    return this.$b;
  }
  , loop:function(times){
    while (times > 0) {
      var u = function(){
        return this.set_b(this.get_b() + 1);
      }
      ;
      u(times--);
    }
  }
  , box:function(){
    this.loop(5);
    return this.get_b() === 5;
  }
  });
}



function test() {
     return foo.box()
}
