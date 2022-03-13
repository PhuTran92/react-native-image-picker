package com.imagepicker.picker

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.imagepicker.R
import com.imagepicker.picker.adapter.ImageListAdapter
import com.imagepicker.picker.model.Image
import com.imagepicker.utils.DisplayUtils

class ImageSelectionFragment : Fragment() {

    private var rvImageList: RecyclerView? = null
    private var prgLoading: ProgressBar? = null
    private var callback: ImageSelectionCallback? = null
    private var imageListAdapter: ImageListAdapter? = null
    private lateinit var viewModel: ImageListViewModel

    private val selectionLimit by lazy {
        activity?.intent?.getIntExtra(ImageListActivity.SELECTION_LIMIT, -1) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_image_selection, container, false)
        rvImageList = view.findViewById(R.id.rvImageList)
        prgLoading = view.findViewById(R.id.prgLoading)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity(), ViewModelProvider.AndroidViewModelFactory(activity!!.application))[ImageListViewModel::class.java]

        val deviceWidth = DisplayUtils.getScreenWidth(requireActivity())
        val spanCount = (deviceWidth.toFloat() / resources.getDimensionPixelSize(R.dimen.column_max_width)).toInt()

        callback = activity as? ImageSelectionCallback
        rvImageList?.layoutManager = GridLayoutManager(rvImageList!!.context, spanCount)
        rvImageList?.addItemDecoration(object : RecyclerView.ItemDecoration() {
            val spacing = DisplayUtils.dip2px(rvImageList!!.context, 6f)
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val position: Int = parent.getChildAdapterPosition(view)
                val column: Int = position % 3

                outRect.left = spacing - column * spacing / spanCount // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (column + 1) * spacing / spanCount // (column + 1) * ((1f / spanCount) * spacing)

                if (position < spanCount) { // top edge
                    outRect.top = spacing
                }
                outRect.bottom = spacing
            }
        })
        imageListAdapter = ImageListAdapter()
        rvImageList?.adapter = imageListAdapter
        imageListAdapter?.listener = ImageListAdapter.Listener { _, item ->
            checkedImage(item)
        }
        viewModel.loadingEvent.observe(viewLifecycleOwner,
            { aBoolean ->
                if (aBoolean) {
                    prgLoading?.visibility = View.VISIBLE
                    rvImageList?.visibility = View.GONE
                } else {
                    prgLoading?.visibility = View.GONE
                    rvImageList?.visibility = View.VISIBLE
                }
            })
        viewModel.selectedFolder.observe(
            viewLifecycleOwner,
            { folder -> imageListAdapter?.setData(folder?.images ?: emptyList(), viewModel.selectImageList) })
    }

    private fun checkedImage(image: Image?) {
        if (image != null) {
            if (viewModel.selectImageList.contains(image.path)) {
                viewModel.selectImageList.remove(image.path)
                if (callback != null) {
                    callback?.onImageUnselected(image.path)
                }
            } else {
                if (selectionLimit != -1 && viewModel.selectImageList.size >= selectionLimit) return

                viewModel.selectImageList.add(image.path)
                if (callback != null) {
                    callback?.onImageSelected(image.path)
                }
            }

            imageListAdapter?.setData(viewModel.selectedFolder.value?.images ?: emptyList(), viewModel.selectImageList)
        }
    }
}