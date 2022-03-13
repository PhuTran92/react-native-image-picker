package com.imagepicker.picker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.imagepicker.R
import com.imagepicker.picker.adapter.FolderListAdapter
import kotlinx.coroutines.flow.combine

class FolderSelectionFragment: Fragment() {

    private var rvImageList: RecyclerView? = null
    private var prgLoading: ProgressBar? = null
    private var callback: FolderSelectionCallback? = null
    private var folderAdapter: FolderListAdapter? = null
    private lateinit var viewModel: ImageListViewModel

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

        callback = activity as? FolderSelectionCallback
        rvImageList?.layoutManager = LinearLayoutManager(rvImageList!!.context, LinearLayoutManager.VERTICAL, false)

        folderAdapter = FolderListAdapter()
        rvImageList?.adapter = folderAdapter

        val dividerItemDecoration = DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        rvImageList?.addItemDecoration(dividerItemDecoration)

        folderAdapter?.listener = FolderListAdapter.Listener { _, item ->
            viewModel.selectFolder(item)
            callback?.onFolderSelected(item)
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

        combine(viewModel.folders.asFlow(), viewModel.selectedFolder.asFlow()) { folders, selectedFolder ->
            Pair(folders, selectedFolder)
        }.asLiveData().observe(viewLifecycleOwner, {
            folderAdapter?.submitData(it.first, it.second)
        })
    }
}