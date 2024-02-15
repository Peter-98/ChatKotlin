package com.pedmar.chatkotlin.model

class Chat {
    private var keyMessage : String = ""
    private var issuer : String = ""
    private var receiver : String = ""
    private var message : String = ""
    private var url : String = ""
    private var viewed = false
    private var groupChat = false

    constructor()

    constructor(
        keyMessage: String,
        issuer: String,
        receiver: String,
        message: String,
        url: String,
        viewed: Boolean,
        groupChat: Boolean
    ) {
        this.keyMessage = keyMessage
        this.issuer = issuer
        this.receiver = receiver
        this.message = message
        this.url = url
        this.viewed = viewed
        this.groupChat = groupChat
    }

    //getters y setters
    fun getKeyMessage() : String?{
        return keyMessage
    }

    fun setKeyMessage(keyMessage : String?){
        this.keyMessage = keyMessage!!
    }

    fun getIssuer() : String?{
        return issuer
    }

    fun setIssuer(issuer : String?){
        this.issuer = issuer!!
    }

    fun getReceiver() : String?{
        return receiver
    }

    fun setReceiver(receiver : String?){
        this.receiver = receiver!!
    }

    fun getMessage() : String?{
        return message
    }

    fun setMessage(message : String?){
        this.message = message!!
    }

    fun getUrl() : String?{
        return url
    }

    fun setUrl(url : String?){
        this.url = url!!
    }

    fun isViewed() : Boolean{
        return viewed
    }

    fun setIsViewed(viewed : Boolean?){
        this.viewed = viewed!!
    }

    fun isGroupChat() : Boolean{
        return groupChat
    }

    fun setIsGroupChat(groupChat : Boolean?){
        this.groupChat = groupChat!!
    }
}