package com.droiddeck.launcher.gpu
import android.content.Context
object LsfgNative { const val STATUS_OK=0; const val STATUS_NOT_INSTALLED=1 }
object Lossless { fun sync(context: Context)=LsfgNative.STATUS_NOT_INSTALLED; fun cacheFile(context: Context): java.io.File?=null }
object LightFrameGenRuntime { val generating=false; var running=false; fun start(context:Context,hz:Float) {running=true}; fun stop(){running=false} }
