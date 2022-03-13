package com.imagepicker.picker;

import java.io.Serializable;

public interface ImageSelectionCallback extends Serializable {

    void onImageSelected(String path);

    void onImageUnselected(String path);
}
