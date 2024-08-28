package com.yndongyong.androiddevhelper.findact

import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.ide.util.TreeClassChooserFactory
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.search.GlobalSearchScope
import com.yndongyong.androiddevhelper.utils.AdbHelper
import com.yndongyong.androiddevhelper.utils.UIUtils
import org.jetbrains.android.sdk.AndroidSdkUtils

/**
 * 查找Activity 以及包含的 Fragment
 * 适配了Android11及以下，Android 12、Android 13、Android 14
 */
class FindActivityAction : AnAction() {

    /** The path of ADB */
    private var adbPath = ""

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // 获取 adbPath
        adbPath = AndroidSdkUtils.findAdb(project).adbPath.toString().takeIf { it.isNotBlank() }
            ?: return UIUtils.showErrorNotification(project, "Please check your adb environment is correct!")

        // 获取 Android 版本
        val androidVersion = AdbHelper.execCMD("$adbPath shell getprop ro.build.version.release").trim().takeIf { it.isNotBlank() }
            ?: return UIUtils.showErrorNotification(project, "Please check your adb environment is correct!")

        val version = androidVersion.toIntOrNull()
            ?: return UIUtils.showErrorNotification(project, "Invalid Android version!")

        val find: Find = when {
            version >= 12 -> Android12Find(adbPath)
            else -> Android11Find(adbPath)
        }

        // 查找当前 Activity
        val (activityPackage, activity) = find.findActivity()
        if (activityPackage.isBlank()) {
            return UIUtils.showErrorNotification(project, "Please ensure your phone is properly connected via USB")
        }

        if (activityPackage.contains(".launcher/")) {
            return UIUtils.showErrorNotification(
                project,
                "Please open the app associated with this project on your mobile phone to continue."
            )
        }

        // 处理 Fragment 列表
        val fragments = findFragments(adbPath, activityPackage)
        fragments.removeAll(listOf("ReportFragment", "SupportRequestManagerFragment", "AutofillManager"))
        fragments.add(0, activity)
        println(fragments)

        // 显示选择对话框
        try {
            val scope = GlobalSearchScope.allScope(project)
            val chooser = TreeClassChooserFactory.getInstance(project)
                .createNoInnerClassesScopeChooser(
                    "Choose Class to Navigate",
                    scope,
                    { fragments.contains(it.name) },
                    null
                )
            chooser.showDialog()
            chooser.selected?.let { NavigationUtil.activateFileWithPsiElement(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 抽取重复的 Fragment 处理逻辑
    private fun findFragments(adbPath: String, activityPackage: String): MutableList<String?> {
        val fragments = mutableListOf<String?>()
        val result = AdbHelper.execCMD("$adbPath shell dumpsys activity $activityPackage")
        var flag = 0

        result.lines().asReversed().forEach { line ->
            when {
                line.contains("#") && flag == 0 -> flag++
                line.startsWith("Added Fragments:") -> flag++
                flag == 1 && line.trim().startsWith("#")  && line.trim().drop(1).takeWhile { it.isDigit() }.isNotEmpty() -> {
                    val fragmentName = line.trim().substringAfter(" ").substringBefore("{")
                    if (!fragments.contains(fragmentName)) {
                        fragments.add(fragmentName)
                    }
                }
                line.startsWith("AutofillManager:") -> return@forEach
            }
        }
        return fragments
    }

    interface Find {
        /**
         * 查找当前activity,
         * 返回[包名，activity的name]
         */
        fun findActivity(): Pair<String, String>
    }


    // 抽取 Find 的公共逻辑
    abstract class BaseFind(private val adbPath: String) : Find {
        protected fun extractActivity(result: String): Pair<String, String> {
//            //mResumedActivity: ActivityRecord{2baae81 u0 com.xxxx.xxxx/.ui.MainActivity t2431 d0}  android12
//            topResumedActivity=ActivityRecord{47e05a1 u0 com.xxxxx.xxxx/.ui.MainActivity} t494 d0}  android13
            //topResumedActivity=ActivityRecord{77b3dc7 u0 com.xxxxx.xxxx/.ui.MainActivity t90}       android14
            val activityPackage = result.substringAfter("/").substringBefore(" ").replace("}","")
            val activity = activityPackage.substringAfterLast(".")
            println(activityPackage)
            println(activity)
            return Pair(activityPackage, activity)
        }
    }

    class Android11Find(val adbPath: String) : BaseFind(adbPath) {
        override fun findActivity(): Pair<String, String> {
            val result = AdbHelper.execCMD("$adbPath shell dumpsys activity activities | grep mResumedActivity")
            return extractActivity(result)
        }
    }

    class Android12Find(val adbPath: String) : BaseFind(adbPath) {
        override fun findActivity(): Pair<String, String> {
            val result = AdbHelper.execCMD("$adbPath shell dumpsys activity activities | grep topResumedActivity")
            return extractActivity(result)
        }
    }
}