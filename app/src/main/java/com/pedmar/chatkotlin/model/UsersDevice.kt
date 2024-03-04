package com.pedmar.chatkotlin.model

class UsersDevice {

    private var uid : String = ""
    private var idDevice: String = ""
    private var userIdToken: String = ""
    private var enable: Boolean = false

    constructor()

    constructor(uid : String, idDevice: String, userIdToken: String, enable: Boolean){
        this.uid = uid
        this.idDevice = idDevice
        this.userIdToken = userIdToken
        this.enable = enable
    }


    fun getUid() : String{
        return this.uid
    }

    fun setUid(uid : String){
        this.uid = uid
    }

    fun getIdDevice() : String{
        return idDevice
    }

    fun setIdDevice(idDevice : String){
        this.idDevice = idDevice
    }

    fun getUserIdToken() : String{
        return userIdToken
    }

    fun setUserIdToken(userIdToken : String){
        this.userIdToken = userIdToken
    }

    fun isEnable() : Boolean{
        return enable
    }

    fun setEnable(enable : Boolean){
        this.enable = enable
    }
}