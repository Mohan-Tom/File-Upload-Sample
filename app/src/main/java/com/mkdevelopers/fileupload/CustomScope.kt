package com.mkdevelopers.fileupload

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object CustomScope {

    private val TAG = CustomScope::class.java.simpleName

    fun getApplicationScope(): CoroutineScope {
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
            println("$TAG >> customCoroutineScopeException $throwable")
        }

        val context = Dispatchers.IO + SupervisorJob() + coroutineExceptionHandler

        return CoroutineScope(context)
    }
}