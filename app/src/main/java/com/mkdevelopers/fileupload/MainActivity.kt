package com.mkdevelopers.fileupload

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import com.mkdevelopers.fileupload.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import okio.IOException
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: FileViewModel by viewModels()
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    @Inject
    lateinit var workManagerHelper: WorkManagerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        binding.btnUpload.setOnClickListener {
            //val file = File(cacheDir, "my_beautiful_workplace.jpg")
            /*val file = File(externalCacheDir, "my_videoplayback.mp4")
            file.createNewFile()
            file.outputStream().use {
                //assets.open("beautiful-workplace.jpg").copyTo(it)
                assets.open("videoplayback.mp4").copyTo(it)
            }*/

            //viewModel.uploadFile(file)

            workManagerHelper.scheduleToUploadFiles()
        }

        binding.btnCancel.setOnClickListener {
            //viewModel.childScope1?.cancel(CancellationException("job was cancelled by user"))
            workManagerHelper.cancelUploadWork()
        }
    }
}