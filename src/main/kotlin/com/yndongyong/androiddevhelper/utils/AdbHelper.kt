package com.yndongyong.androiddevhelper.utils

import java.io.BufferedReader
import java.io.InputStreamReader

object AdbHelper {

    fun execCMD(command: String): String {
        val sb = StringBuilder()
        try {
            val process = Runtime.getRuntime().exec(command)
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }
        } catch (e: Exception) {
            return e.toString()
        }
        return sb.toString()
    }


}