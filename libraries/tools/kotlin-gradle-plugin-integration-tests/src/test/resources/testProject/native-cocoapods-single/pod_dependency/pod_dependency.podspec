Pod::Spec.new do |spec|
    spec.name                     = 'pod_dependency'
    spec.version                  = '1.0'
    spec.homepage                 = 'foo'
    spec.source                   = { :git => "Not Published", :tag => "Cocoapods/#{spec.name}/#{spec.version}" }
    spec.authors                  = ''
    spec.license                  = ''
    spec.summary                  = 'foo'
    spec.source_files             = 'src/*'
    spec.public_header_files      = 'src/*.h'

    spec.ios.deployment_target = '11.0'
end