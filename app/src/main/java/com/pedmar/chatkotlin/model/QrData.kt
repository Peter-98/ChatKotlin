package com.pedmar.chatkotlin.model

class QrData {

    private var qrDataShare : String = ""
    private var qrLogin: String = ""

    constructor()

    constructor(qrDataShare : String, qrLogin: String){
        this.qrDataShare = qrDataShare
        this.qrLogin = qrLogin
    }


    fun getQrDataShare() : String{
        return qrDataShare
    }

    fun setQrDataShare(qrDataShare : String){
        this.qrDataShare = qrDataShare!!
    }

    fun getQrLogin() : String{
        return qrLogin
    }

    fun setQrLogin(qrLogin : String){
        this.qrLogin = qrLogin
    }
}