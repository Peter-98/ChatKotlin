package com.pedmar.chatkotlin.group

import com.pedmar.chatkotlin.model.GroupChat

object ActivityManager {
    private var createGroupActivity: CreateGroupActivity? = null

    fun setCreateGroupActivity(activity: CreateGroupActivity) {
        createGroupActivity = activity
    }

    fun callCreateGroup(groupChat: GroupChat) {
        createGroupActivity?.createGroup(groupChat)
    }
}