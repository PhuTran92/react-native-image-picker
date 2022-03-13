package com.imagepicker.picker

import com.imagepicker.picker.model.Folder

interface FolderSelectionCallback {

    fun onFolderSelected(folder: Folder)
}