Pod::Spec.new do |spec|
    spec.name                     = 'subspec_dependency'
    spec.version                  = '1.0'
    spec.homepage                 = 'baz'
    spec.source                   = { :git => "Not Published", :tag => "Cocoapods/#{spec.name}/#{spec.version}" }
    spec.authors                  = ''
    spec.license                  = ''
    spec.summary                  = 'baz'
    spec.default_subspec          = 'Core'

    spec.subspec 'Core' do |core|
        core.source_files = "src/*"
        core.public_header_files = "src/*.h"
    end

    spec.ios.deployment_target = '11.0'
end