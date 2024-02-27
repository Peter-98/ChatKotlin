package com.pedmar.chatkotlin.group

import com.pedmar.chatkotlin.QrCodeActivity
import com.pedmar.chatkotlin.model.GroupChat

object ActivityManager {
    private var createGroupActivity: CreateGroupActivity? = null
    private var qrCodeActivity: QrCodeActivity? = null

    fun setCreateGroupActivity(activity: CreateGroupActivity) {
        createGroupActivity = activity
    }

    fun callCreateGroup(groupChat: GroupChat) {
        createGroupActivity?.createGroup(groupChat)
    }

    fun setQrCodeActivity(activity: QrCodeActivity) {
        qrCodeActivity = activity
    }

    fun callSaveQrDataUser(data: String) {
        qrCodeActivity?.getQrDataFromUniqueUrl(data)
    }


}