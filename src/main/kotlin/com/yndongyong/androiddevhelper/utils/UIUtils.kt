package com.yndongyong.androiddevhelper.utils

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object UIUtils {

    fun showErrorNotification(project: Project, content: String) {
        NotificationGroupManager.getInstance().getNotificationGroup("DongAndroidDevHelper Notification Group")
            .createNotification(content, NotificationType.ERROR)
            .notify(project)
    }

    fun showInfoNotification(project: Project, content: String) {
        NotificationGroupManager.getInstance().getNotificationGroup("DongAndroidDevHelper Notification Group")
            .createNotification(content, NotificationType.INFORMATION)
            .notify(project)
    }
}