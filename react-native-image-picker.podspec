require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = package['name']
  s.version      = package['version']
  s.summary      = package['description']
  s.license      = package['license']

  s.authors      = package['author']
  s.homepage     = package['homepage']
  s.platform     = :ios, "9.1"

  s.source       = { :git => "https://github.com/react-native-image-picker/react-native-image-picker.git", :tag => "v#{s.version}" }

  s.source_files = 'ios/*.{swift,h,m}'
  s.resource_bundles = { 'TLPhotoPicker' => ['TLPhotoPicker/Classes/*.xib'] }
  s.resources = 'TLPhotoPicker/TLPhotoPickerController.bundle'
  s.public_header_files = 'ios/*.h'

  s.dependency 'React-Core'
end
