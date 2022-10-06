Pod::Spec.new do |spec|
    spec.name                     = 'grooframe'
    spec.version                  = '0.2'
    spec.vendored_frameworks      = 'grooframe.framework'

    spec.license                  = 'Apache-2.0'
    spec.homepage                 = 'https://example.com/lib'
    spec.ios.deployment_target    = "10.0"
    spec.swift_versions           = ['4', '5']

    # This is raw statement that is appended 'as is' to the podspec
    spec.frameworks = 'CFNetwork'
end
