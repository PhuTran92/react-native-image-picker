package com.imagepicker.picker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.imagepicker.R
import com.imagepicker.picker.model.Folder

class ImageListActivity: FragmentActivity(), View.OnClickListener,
    ImageSelectionCallback, FolderSelectionCallback {

    companion object {
        const val INTENT_RESULT = "result"
        const val SELECTION_LIMIT = "selection_limit"

        fun startForResult(activity: Activity, requestCode: Int, selectionLimit: Int) {
            val intent = Intent(activity, ImageListActivity::class.java)
            intent.putExtra(SELECTION_LIMIT, selectionLimit)
            activity.startActivityForResult(intent, requestCode)
        }
    }

    private var btnConfirm: TextView? = null
    private var imgDropdown: ImageView? = null
    private var lnlFolder: View? = null
    private var tvTitle: TextView? = null
    private val result = ArrayList<String>()

    private lateinit var viewModel: ImageListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_list)

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimaryDark)

        val tvCurrentFolder = findViewById<TextView>(R.id.tvCurrentFolder)
        tvTitle = findViewById(R.id.tvTitle)

        btnConfirm = findViewById(R.id.btnConfirm)
        btnConfirm?.setOnClickListener(this)

        lnlFolder = findViewById(R.id.lnlFolder)
        lnlFolder?.setOnClickListener(this)

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        ivBack?.setOnClickListener(this)

        imgDropdown = findViewById(R.id.imgIconDropdown)

        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(application)).get(
            ImageListViewModel::class.java
        )
        viewModel.selectedFolder.observe(this, {
            it?.run {
                tvCurrentFolder.text = this.name
            }
        })
        viewModel.folders.observe(this, {
            lnlFolder?.visibility = if (!it.isNullOrEmpty() && it.size > 1) View.VISIBLE else View.GONE
        })

        hideFolderFragment()

        // refresh confirm button
        refreshViews()
    }

    private fun showFolderFragment() {
        supportFragmentManager.findFragmentById(R.id.fmFolders)?.run {
            supportFragmentManager.beginTransaction().show(this).commitAllowingStateLoss()
        }
        imgDropdown?.rotation = 180f
    }

    private fun hideFolderFragment() {
        supportFragmentManager.findFragmentById(R.id.fmFolders)?.run {
            supportFragmentManager.beginTransaction().hide(this).commitAllowingStateLoss()
        }
        imgDropdown?.rotation = 0f
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.lnlFolder -> {
                if (imgDropdown?.rotation == 0f) {
                    showFolderFragment()
                } else {
                    hideFolderFragment()
                }
            }
            R.id.btnConfirm -> exit()
            R.id.ivBack -> onBackPressed()
        }
    }

    override fun onImageSelected(path: String) {
        refreshViews()
    }

    override fun onImageUnselected(path: String) {
        refreshViews()
    }

    private fun refreshViews() {
        btnConfirm?.isEnabled = viewModel.selectImageList.isNotEmpty()
        btnConfirm?.alpha = if (btnConfirm?.isEnabled == true) 1f else 0.5f
        tvTitle?.text = if (btnConfirm?.isEnabled == true) String.format(getString(R.string.select_images_format),
            viewModel.selectImageList.size
        ) else getString(R.string.select_image_files)
    }

    private fun exit() {
        val intent = Intent()
        result.clear()
        result.addAll(viewModel.selectImageList)
        intent.putStringArrayListExtra(INTENT_RESULT, result)
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onFolderSelected(folder: Folder) {
        hideFolderFragment()
    }
}