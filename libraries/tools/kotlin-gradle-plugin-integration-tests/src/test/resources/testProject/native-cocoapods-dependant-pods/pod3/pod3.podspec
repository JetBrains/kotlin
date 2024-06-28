Pod::Spec.new do |spec|
    spec.name                     = 'pod3'
    spec.version                  = '1.0'
    spec.homepage                 = 'repro'
    spec.source                   = { :git => "Not Published", :tag => "Cocoapods/#{spec.name}/#{spec.version}" }
    spec.authors                  = ''
    spec.license                  = ''
    spec.summary                  = 'repro'

    spec.source_files = '*.{h,m}'
    spec.public_header_files = '*.h'

    spec.dependency 'pod1'
end