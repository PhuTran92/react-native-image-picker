//
//  ImagePickerManager.swift
//  react-native-image-picker
//
//  Created by Phu Tran on 3/17/22.
//

import Foundation
import AVFoundation
import Photos
import PhotosUI
import MobileCoreServices

let errCameraUnavailable = "camera_unavailable";
let errPermission = "permission";
let errOthers = "others";
var target: ImagePickerTarget? = nil

@objc(ImagePickerManager)
class ImagePickerManager: NSObject {
    
    private var callback: RCTResponseSenderBlock? = nil
    private var options: [String: AnyObject] = [String: AnyObject]()

    @objc func launchCamera(_ options: [String: AnyObject], callback: @escaping RCTResponseSenderBlock) {
        target = .camera
        DispatchQueue.main.async {
            self.launchImagePicker(options: options, callback: callback)
        }
    }
    
    @objc func launchImageLibrary(_ options: [String: AnyObject], callback: @escaping RCTResponseSenderBlock) {
        target = .library
        DispatchQueue.main.async {
            self.launchImagePicker(options: options, callback: callback)
        }
    }
    
    private func launchImagePicker(options: [String: AnyObject], callback: @escaping RCTResponseSenderBlock) -> Void {
        self.callback = callback
        
        if (target == .camera && ImagePickerUtils.isSimulator()) {
            self.callback?([["errorCode": errCameraUnavailable]])
        }
        
        self.options = options
        
//        #if canImport(PhotosUI)
//        if #available(iOS 14, *) {
//            if (target == .library) {
//                let configuration = ImagePickerUtils.makeConfiguration(fromOptions: options, target: target!)
//                if let configuration = configuration {
//                    let picker = PHPickerViewController(configuration: configuration)
//                    picker.delegate = self
//                    picker.presentationController?.delegate = self
//
//                    self.showPickerViewController(picker: picker)
//                    return
//                }
//            }
//        }
//        #endif
        
        if target == .camera {
            let picker: UIImagePickerController = UIImagePickerController()
            ImagePickerUtils.setupPickerFromOptions(picker: picker, options: self.options, target: target!)
            picker.delegate = self
            self.checkPermission { (granted) in
                if !granted {
                    self.callback?([["errorCode", errPermission]])
                    return
                }
                self.showPickerViewController(picker: picker)
            }
        } else {
            self.showCustomPickerController()
        }
    }
    
    private func showPickerViewController(picker: UIViewController) {
        DispatchQueue.main.async {
            let root = RCTPresentedViewController()
            root?.present(picker, animated: true, completion: nil)
        }
    }
    
    private func showCustomPickerController() {
        DispatchQueue.main.async {
            let root = RCTPresentedViewController()
            
            let viewController = TLPhotosPickerViewController()
            viewController.modalPresentationStyle = .fullScreen
            
            var configure = TLPhotosPickerConfigure()
            configure.numberOfColumn = 3
            configure.allowedVideo = false
            configure.mediaType = .image
            configure.usedCameraButton = false
            configure.autoPlay = false
            configure.allowedAlbumCloudShared = false
            configure.allowedVideoRecording = false
    
            viewController.configure = configure
   
            root?.present(viewController, animated: true, completion: nil)
        }
    }

    private func mapImageToAsset(image: UIImage, data: Data?) -> [String: Any] {
        var newImage: UIImage? = nil
        var newData: Data? = nil
        
        let fileType = ImagePickerUtils.getFileType(imageData: data)
        if let saveToPhotos = self.options["saveToPhotos"] as? Bool, saveToPhotos, target == .camera {
            UIImageWriteToSavedPhotosAlbum(image, nil, nil, nil);
        }
        
        if let maxWidth = self.options["maxWidth"] as? Float, let maxHeight = self.options["maxHeight"] as? Float, fileType == "gif" {
            newImage = ImagePickerUtils.resizeImage(image: image, maxWidth: maxWidth, maxHeight: maxHeight)
        }
        
        if let quality = self.options["quality"] as? Float, fileType == "jpg" {
            newData = image.jpegData(compressionQuality: CGFloat(quality))
        } else if fileType == "png" {
            newData = image.pngData()
        }
        
        var asset = [String: Any]()
        asset["type"] = "image/".appending(fileType)
        
        let fileName = self.getImageFileName(fileType: fileType)
        let path = URL(fileURLWithPath: NSTemporaryDirectory(), isDirectory: true).appendingPathComponent(fileName)
        do {
            try newData?.write(to: path)
        } catch {
            print(error)
        }
        
        if let includeBase64 = self.options["includeBase64"] as? Bool, includeBase64 {
            asset["base64"] = newData?.base64EncodedData(options: Data.Base64EncodingOptions(rawValue: 0))
        }
        
        asset["uri"] = path.absoluteString
        asset["fileName"] = fileName;
        
        if let newImage = newImage {
            asset["width"] = NSNumber(value: Int(newImage.size.width))
            asset["height"] = NSNumber(value: Int(newImage.size.height))
        }
        
        return asset
    }
    
    private func mapVideoToAsset(url: URL) -> [String: Any]? {
        let fileName = url.lastPathComponent
        
        let videoDestinationURL = URL(fileURLWithPath: NSTemporaryDirectory(), isDirectory: true).appendingPathComponent(fileName)
        
        if let saveToPhoto = self.options["saveToPhotos"] as? Bool, saveToPhoto, target == .camera {
            UISaveVideoAtPathToSavedPhotosAlbum(url.path, nil, nil, nil)
        }
        
        if url.resolvingSymlinksInPath().path != videoDestinationURL.resolvingSymlinksInPath().path {
            let fileManager = FileManager.default
            if fileManager.fileExists(atPath: videoDestinationURL.path) {
                do {
                    try fileManager.removeItem(at: videoDestinationURL)
                } catch {
                    print(error)
                }
            }
            
            do {
                if fileManager.isWritableFile(atPath: url.path) {
                    try fileManager.moveItem(at: url, to: videoDestinationURL)
                } else {
                    try fileManager.copyItem(at: url, to: videoDestinationURL)
                }
            } catch {
                print(error)
                return nil
            }

            var asset = [String: Any]()
            asset["duration"] = NSNumber(value: CMTimeGetSeconds(AVAsset(url: videoDestinationURL).duration))
            asset["uri"] = videoDestinationURL.absoluteString
            asset["type"] = ImagePickerUtils.getFileType(from: videoDestinationURL)
            
            return asset
        }
        
        return nil
    }
    
    private func checkCameraPermissions(callback: @escaping (Bool) -> Void) {
        let status = AVCaptureDevice.authorizationStatus(for: .video)
        if status == .authorized {
            callback(true)
        } else if status == .denied {
            AVCaptureDevice.requestAccess(for: .video) { (granted) in
                callback(granted)
            }
        } else {
            callback(false)
        }
    }
    
    private func checkPhotosPermissions(callback: @escaping (Bool) -> Void) {
        let status = PHPhotoLibrary.authorizationStatus()
        if status == .authorized {
            callback(true)
        } else if status == .notDetermined {
            PHPhotoLibrary.requestAuthorization { (status) in
                if status == .authorized {
                    callback(true)
                } else {
                    callback(false)
                }
            }
        } else {
            callback(false)
        }
    }
    
    
    private func getImageFileName(fileType: String) -> String {
        var fileName = NSUUID.init().uuidString
        fileName = fileName.appending(".")
        return fileName.appending(fileType)
    }
    
    private func getUIImageFromInfo(info: [UIImagePickerController.InfoKey: Any]) -> UIImage? {
        var image = info[UIImagePickerController.InfoKey.editedImage]
        if image == nil {
            image = info[UIImagePickerController.InfoKey.originalImage]
        }
        return image as? UIImage
    }
    
    private func getNSURLFromInfo(info: [UIImagePickerController.InfoKey: Any]) -> URL? {
        if #available(iOS 11.0, *) {
            return info[UIImagePickerController.InfoKey.imageURL] as? URL
        } else {
            return info[UIImagePickerController.InfoKey.referenceURL] as? URL
        }
    }
    
    // Both camera and photo write permission is required to take picture/video and store it to public photos
    private func checkCameraAndPhotoPermission(callback: @escaping (Bool) -> Void) {
        checkCameraPermissions { [weak self] (cameraGranted) in
            if !cameraGranted {
                callback(false)
                return
            }
            
            self?.checkPhotosPermissions { (photoGranted) in
                if !photoGranted {
                    callback(false)
                    return
                }
                
                callback(true)
            }
        }
    }

    private func checkPermission(callback: @escaping (Bool) -> Void) {
        let permissionBlock: (Bool) -> Void = { permissionGranted in
            if !permissionGranted {
                callback(false)
                return
            }
            
            callback(true)
        }
        
        if let saveToPhoto = self.options["saveToPhotos"] as? Bool, saveToPhoto, target == .camera {
            checkCameraAndPhotoPermission(callback: permissionBlock)
        } else if target == .camera {
            checkCameraPermissions(callback: permissionBlock)
        } else {
            if #available(iOS 11.0, *) {
                callback(true)
            } else {
                checkPhotosPermissions(callback: permissionBlock)
            }
        }
    }
}

extension ImagePickerManager: UIAdaptivePresentationControllerDelegate {
    func presentationControllerDidDismiss(_ presentationController: UIPresentationController) {
        // do nothing
    }
}

extension ImagePickerManager: UIImagePickerControllerDelegate, UINavigationControllerDelegate {
    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
        
        let dismissCompletionBlock: () -> Void = {
            var assets = [[String: Any]]()
            if let mediaType = info[UIImagePickerController.InfoKey.mediaType] as? String, mediaType == kUTTypeImage as String {
                if let image = ImagePickerManager().getUIImageFromInfo(info: info) {
                    do {
                        var data: Data? = nil
                        let url = self.getNSURLFromInfo(info: info)
                        if let url = url {
                            data = try Data(contentsOf: url)
                        }
                        assets.append(self.mapImageToAsset(image: image, data: data))
                    } catch {
                        print(error)
                        self.callback?([["errorCode": errOthers, "errorMessage": error.localizedDescription]])
                    }
                } else {
                    self.callback?([["errorCode": errOthers, "errorMessage":  "imagePickerController image from info is null"]])
                }
            } else {
                if let mediaURL = info[UIImagePickerController.InfoKey.mediaURL] as? URL {
                    let asset = self.mapVideoToAsset(url: mediaURL)
                    if asset == nil {
                        self.callback?([["errorCode": errOthers, "errorMessage":  "imagePickerController asset is null"]])
                        return
                    }
                    assets.append(asset!)
                } else {
                    self.callback?([["errorCode": errOthers, "errorMessage":  "imagePickerController mediaURL is null"]])
                }
            }
            
            var response = [String: Any]()
            response["assets"] = assets
            self.callback?([response])
        }
        
        DispatchQueue.main.async {
            picker.dismiss(animated: true, completion: dismissCompletionBlock)
        }
    }
    
    private func imagePickerControllerDidCancel(picker: UIImagePickerController) {
        DispatchQueue.main.async {
            picker.dismiss(animated: true) { [weak self] in
                self?.callback?([["didCancel": true]])
            }
        }
    }
}

#if canImport(PhotosUI)
@available(iOS 14, *)
extension ImagePickerManager: PHPickerViewControllerDelegate {
    func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
        picker.dismiss(animated: true, completion: nil)
        if results.count == 0 {
            DispatchQueue.main.async {
                self.callback?([["didCancel": true]])
            }
            return
        }
        
        let completionGroup = DispatchGroup()
        var assets = [[String: Any]?](repeating: nil, count: results.count)
        
        for (_, result) in results.enumerated() {
            let provider = result.itemProvider
            completionGroup.enter()
            if provider.hasItemConformingToTypeIdentifier(kUTTypeImage as String) {
                provider.loadDataRepresentation(forTypeIdentifier: kUTTypeImage as String) { (data, error) in
                    if let data = data {
                        let image = UIImage(data: data)
                        assets.append(self.mapImageToAsset(image: image!, data: data))
                        completionGroup.leave()
                    }
                }
            }
            
            if provider.hasItemConformingToTypeIdentifier(kUTTypeMovie as String) {
                provider.loadFileRepresentation(forTypeIdentifier: kUTTypeMovie as String) { (url, error) in
                    if let url = url {
                        assets.append(self.mapVideoToAsset(url: url))
                        completionGroup.leave()
                    }
                }
            }
        }
        
        completionGroup.notify(queue: .main) {
            for (_, asset) in assets.enumerated() {
                if (nil == asset) {
                    self.callback?([["errorCode": errOthers]]);
                    return
                }
            }
            
            var response = [String: Any]()
            response["assets"] = assets
            self.callback?([response])
        }
    }
}
#endif

