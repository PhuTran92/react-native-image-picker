package com.imagepicker.picker

import android.app.Application
import android.content.Context
import android.provider.MediaStore
import android.text.TextUtils
import androidx.lifecycle.*
import androidx.loader.content.CursorLoader
import com.imagepicker.R
import com.imagepicker.picker.model.Folder
import com.imagepicker.picker.model.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class ImageListViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private val IMAGE_PROJECTION: Array<String> = arrayOf(
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media._ID
        )
    }

    val selectImageList: ArrayList<String> = ArrayList()

    val folders: LiveData<List<Folder>> get() = _folders
    private val _folders: MutableLiveData<List<Folder>> = MutableLiveData()

    val selectedFolder: LiveData<Folder?> get() = _selectedFolder
    private val _selectedFolder: MutableLiveData<Folder?> = MutableLiveData<Folder?>()

    val loadingEvent: LiveData<Boolean> get() = _loadingEvent
    private val _loadingEvent: MutableLiveData<Boolean> = MutableLiveData()

    init {
        loadData(getApplication())
    }

    fun selectFolder(folder: Folder) {
        _selectedFolder.value = folder
    }

    private fun loadData(context: Context?) {
        _loadingEvent.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val cursorLoader = withContext(Dispatchers.Main) {
                CursorLoader(
                    (context)!!,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_PROJECTION,
                    null, null, MediaStore.Images.Media.DATE_ADDED + " DESC"
                )
            }
            val data = cursorLoader.loadInBackground()

            val folderList = ArrayList<Folder>()

            if (data != null) {
                val count = data.count
                if (count == 0) {
                    _loadingEvent.postValue(false)
                    return@launch
                }

                val tempImageList = ArrayList<Image>()
                data.moveToFirst()
                do {
                    val path = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[0]))
                    val name = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[1]))
                    val image = Image(path, name)
                    tempImageList.add(image)

                    // divide to folder
                    val imageFile = File(path)
                    val folderFile = imageFile.parentFile
                    if (folderFile == null || !imageFile.exists() || imageFile.length() < 10) {
                        continue
                    }

                    var parent: Folder? = null
                    for (folder in folderList) {
                        if (TextUtils.equals(folder.path, folderFile.absolutePath)) {
                            parent = folder
                        }
                    }
                    if (parent != null) {
                        parent.images.add(image)
                    } else {
                        parent = Folder()
                        parent.name = folderFile.name
                        parent.path = folderFile.absolutePath
                        parent.cover = image

                        val imageList: MutableList<Image> = ArrayList()
                        imageList.add(image)
                        parent.images = imageList

                        folderList.add(parent)
                    }

                } while (data.moveToNext())

                // add all image folder
                val root = Folder()
                root.cover = tempImageList.firstOrNull()
                root.images = ArrayList(tempImageList)
                root.name = context?.getString(R.string.all)
                folderList.add(0, root)

                if (_selectedFolder.value == null) {
                    withContext(Dispatchers.Main) { _selectedFolder.value = folderList.firstOrNull() }
                }
                _folders.postValue(folderList)
                _loadingEvent.postValue(false)
            }
        }
    }
}