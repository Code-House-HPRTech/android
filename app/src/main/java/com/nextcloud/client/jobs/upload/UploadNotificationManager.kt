/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper_ozturk@proton.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.jobs.upload

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.owncloud.android.R
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.theme.ViewThemeUtils

class UploadNotificationManager(private val context: Context, viewThemeUtils: ViewThemeUtils) {
    companion object {
        private const val ID = 411
    }

    private var notification: Notification? = null
    private var notificationBuilder: NotificationCompat.Builder =
        NotificationUtils.newNotificationBuilder(context, viewThemeUtils).apply {
            setContentTitle(context.getString(R.string.foreground_service_upload))
            setSmallIcon(R.drawable.notification_icon)
            setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.notification_icon))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_UPLOAD)
            }
        }
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        notification = notificationBuilder.build()
    }

    @Suppress("MagicNumber")
    fun prepareForStart(
        uploadFileOperation: UploadFileOperation,
        cancelPendingIntent: PendingIntent,
        startIntent: PendingIntent
    ) {
        notificationBuilder.run {
            setContentTitle(context.getString(R.string.uploader_upload_in_progress_ticker))
            setContentText(
                String.format(
                    context.getString(R.string.uploader_upload_in_progress),
                    0,
                    uploadFileOperation.fileName
                )
            )
            setTicker(context.getString(R.string.foreground_service_upload))
            setProgress(100, 0, false)
            setOngoing(true)
            clearActions()

            addAction(
                R.drawable.ic_action_cancel_grey,
                context.getString(R.string.common_cancel),
                cancelPendingIntent
            )

            setContentIntent(startIntent)
        }

        if (!uploadFileOperation.isInstantPicture && !uploadFileOperation.isInstantVideo) {
            showNotification()
        }
    }

    fun notifyForFailedResult(
        resultCode: RemoteOperationResult.ResultCode,
        conflictsResolveIntent: PendingIntent?,
        credentialIntent: PendingIntent?,
        errorMessage: String
    ) {
        val textId = resultTitle(resultCode)

        notificationBuilder.run {
            setTicker(context.getString(textId))
            setContentTitle(context.getString(textId))
            setAutoCancel(false)
            setOngoing(false)
            setProgress(0, 0, false)
            clearActions()

            conflictsResolveIntent?.let {
                addAction(
                    R.drawable.ic_cloud_upload,
                    R.string.upload_list_resolve_conflict,
                    it
                )
            }

            credentialIntent?.let {
                setContentIntent(it)
            }

            setContentText(errorMessage)
        }
    }

    private fun resultTitle(resultCode: RemoteOperationResult.ResultCode): Int {
        val needsToUpdateCredentials = (resultCode == RemoteOperationResult.ResultCode.UNAUTHORIZED)

        return if (needsToUpdateCredentials) {
            R.string.uploader_upload_failed_credentials_error
        } else if (resultCode == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
            R.string.uploader_upload_failed_sync_conflict_error
        } else {
            R.string.uploader_upload_failed_ticker
        }
    }

    fun addAction(icon: Int, textId: Int, intent: PendingIntent) {
        notificationBuilder.addAction(
            icon,
            context.getString(textId),
            intent
        )
    }

    fun showNewNotification(operation: UploadFileOperation) {
        notificationManager.notify(
            NotificationUtils.createUploadNotificationTag(operation.file),
            FileUploadWorker.NOTIFICATION_ERROR_ID,
            notificationBuilder.build()
        )
    }

    private fun showNotification() {
        notificationManager.notify(ID, notificationBuilder.build())
    }

    @Suppress("MagicNumber")
    fun updateUploadProgress(filename: String, percent: Int, currentOperation: UploadFileOperation?) {
        notificationBuilder.run {
            setProgress(100, percent, false)
            val text = String.format(context.getString(R.string.uploader_upload_in_progress), percent, filename)
            setContentText(text)

            showNotification()
            dismissOldErrorNotification(currentOperation)
        }
    }

    fun dismissOldErrorNotification(operation: UploadFileOperation?) {
        if (operation == null) {
            return
        }

        dismissOldErrorNotification(operation.file.remotePath, operation.file.storagePath)

        operation.oldFile?.let {
            dismissOldErrorNotification(it.remotePath, it.storagePath)
        }
    }

    fun dismissOldErrorNotification(remotePath: String, localPath: String) {
        notificationManager.cancel(
            NotificationUtils.createUploadNotificationTag(remotePath, localPath),
            FileUploadWorker.NOTIFICATION_ERROR_ID
        )
    }

    fun dismissWorkerNotifications() {
        notificationManager.cancel(ID)
    }

    fun notifyPaused(intent: PendingIntent) {
        notificationBuilder.apply {
            setContentTitle(context.getString(R.string.upload_global_pause_title))
            setTicker(context.getString(R.string.upload_global_pause_title))
            setOngoing(true)
            setAutoCancel(false)
            setProgress(0, 0, false)
            clearActions()
            setContentIntent(intent)
        }

        showNotification()
    }
}
