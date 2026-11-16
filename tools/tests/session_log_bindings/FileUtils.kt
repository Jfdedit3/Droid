package com.droiddeck.launcher.core
import java.io.File
object FileUtils { fun delete(file: File) { file.deleteRecursively() } }
