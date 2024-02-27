package com.pedmar.chatkotlin.model

class Chat {
    private var keyMessage : String = ""
    private var issuer : String = ""
    private var usernameIssuer : String = ""
    private var receiver : String = ""
    private var message : String = ""
    private var url : String = ""
    private var viewed = false
    private var groupChat = false
    private var allViewed: List<Boolean> = emptyList()
    private var image : String = ""

    constructor()

    constructor(
        keyMessage: String,
        issuer: String,
        usernameIssuer: String,
        receiver: String,
        message: String,
        url: String,
        viewed: Boolean,
        groupChat: Boolean,
        allViewed: List<Boolean>,
        image: String
    ) {
        this.keyMessage = keyMessage
        this.issuer = issuer
        this.usernameIssuer = usernameIssuer
        this.receiver = receiver
        this.message = message
        this.url = url
        this.viewed = viewed
        this.groupChat = groupChat
        this.allViewed = allViewed
        this.image = image
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

    fun getUsernameIssuer() : String?{
        return usernameIssuer
    }

    fun setUsernameIssuer(usernameIssuer : String?){
        this.usernameIssuer = usernameIssuer!!
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

    fun getAllViewed() : List<Boolean>?{
        return allViewed
    }

    fun setAllViewed(allViewed : List<Boolean>?){
        this.allViewed = allViewed!!
    }

    fun getImage() : String{
        return image
    }

    fun setImage(image : String){
        this.image = image!!
    }
}