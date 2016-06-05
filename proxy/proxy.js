var protobuf = require('protocol-buffers')
var fs = require('fs')

var messages = protobuf(fs.readFileSync(__dirname + '/../proto/carkot.proto'))

var ex = {
  data: '12345'
}

var buf = messages.Upload.encode(ex)
