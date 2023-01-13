Pod::Spec.new do |spec|
    spec.name                     = 'myframe'
    spec.version                  = '0.1'
    spec.vendored_frameworks      = 'myframe.framework'

    spec.prefix_header_file       = false
end
