//
//  ImagePickerUtils.swift
//  react-native-image-picker
//
//  Created by Phu Tran on 3/17/22.
//

import Foundation
import PhotosUI
import CoreServices

enum ImagePickerTarget: Int {
    case camera = 1,
    library
}


class ImagePickerUtils {
    
    static func setupPickerFromOptions(picker: UIImagePickerController, options: [String: AnyObject], target: ImagePickerTarget) {
        if let mediaType = options["mediaType"] as? String, mediaType == "video" {
            if let videoQuality = options["videoQuality"] as? String {
                if videoQuality == "high" {
                    picker.videoQuality = .typeHigh
                } else if videoQuality == "low" {
                    picker.videoQuality = .typeLow
                } else {
                    picker.videoQuality = .typeLow
                }
            }
        }
        
        if target == .camera {
            picker.sourceType = .camera
            
            if let durationLimit = options["durationLimit"] as? Double, durationLimit > 0 {
                picker.videoMaximumDuration = durationLimit
            }
            if let cameraType = options["cameraType"] as? String, cameraType == "front" {
                picker.cameraDevice = .front
            } else {
                picker.cameraDevice = .rear
            }
        } else {
            picker.sourceType = .photoLibrary
        }
        
        if let mediaType = options["mediaType"] as? String {
            if mediaType == "video" {
                picker.mediaTypes = [kUTTypeMovie as String]
            } else if mediaType == "photo" {
                picker.mediaTypes = [kUTTypeImage as String]
            } else if target == .library && mediaType == "mixed" {
                picker.mediaTypes = [kUTTypeImage as String, kUTTypeMovie as String]
            }
        }
        
        picker.modalPresentationStyle = .currentContext
    }
    
    @available(iOS 14, *)
    static func makeConfiguration(fromOptions options: [String: AnyObject], target: ImagePickerTarget) -> PHPickerConfiguration? {
        #if canImport(PhotosUI)
        var configuration = PHPickerConfiguration()
        configuration.preferredAssetRepresentationMode = PHPickerConfiguration.AssetRepresentationMode.current
        configuration.selectionLimit = options["selectionLimit"] as! Int
        if let mediaType = options["mediaType"] as? String {
            if mediaType == "video" {
                configuration.filter = PHPickerFilter.videos
            } else if mediaType == "photo" {
                configuration.filter = PHPickerFilter.images
            } else if target == .library && mediaType == "mixed" {
                configuration.filter = PHPickerFilter.any(of: [PHPickerFilter.images, PHPickerFilter.videos])
            }
        }
        return configuration
        #else
        return nil
        #endif
    }
    
    static func isSimulator() -> Bool {
        #if TARGET_OS_IPHONE
        return true
        #endif
        return false
    }
    
    static func getFileType(imageData: Data?) -> String {
        guard let imageData = imageData else {
            return "jpg"
        }
        
        var firstByte: UInt8 = 0
        imageData.copyBytes(to: &firstByte, count: 1)
        switch firstByte {
            case 0xFF:
                return "jpg"
            case 0x89:
                return "png"
            case 0x47:
                return "gif"
            default:
                return "jpg"
        }

    }
    
    static func getFileType(from url: URL) -> String {
        return url.pathExtension
    }
    
    static func resizeImage(image: UIImage, maxWidth: Float, maxHeight: Float) -> UIImage? {
        if maxWidth == 0 || maxWidth == 0 {
            return image
        }
        
        if Float(image.size.width) <= maxWidth, Float(image.size.height) <= maxHeight {
            return image
        }
        
        var newSize = CGSize.init(width: image.size.width, height: image.size.height)
        if maxWidth < Float(newSize.width) {
            newSize = CGSize.init(width: CGFloat(maxWidth), height: (CGFloat(maxWidth) / newSize.width) * newSize.height)
        }
        if maxHeight < Float(newSize.height) {
            newSize = CGSize.init(width: (CGFloat(maxHeight) / newSize.height) * newSize.width, height: CGFloat(maxHeight))
        }
        
        newSize.width = CGFloat(Int(newSize.width))
        newSize.height = CGFloat(Int(newSize.height))
        
        UIGraphicsBeginImageContext(newSize)
        image.draw(in: CGRect.init(x: 0, y: 0, width: Int(newSize.width), height: Int(newSize.height)))
        let newImage = UIGraphicsGetImageFromCurrentImageContext()
        if newImage == nil {
            print("could not scale image")
        }
        UIGraphicsEndImageContext()
        return newImage
    }
}
