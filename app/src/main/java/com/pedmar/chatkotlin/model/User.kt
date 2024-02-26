package com.pedmar.chatkotlin.model

class User {

    private var uid : String = ""
    private var username : String = ""
    private var email : String = ""
    private var image : String = ""
    private var search : String = ""
    private var name : String = ""
    private var surnames : String = ""
    private var age : String = ""
    private var phone : String = ""
    private var status : String = ""
    private var provider : String = ""
    private var private : Boolean = true
    private var knownPrivateUsers : List<String> = emptyList()
    private var qrMark : String = ""

    constructor()

    constructor(
        uid: String,
        email: String,
        image: String,
        search: String,
        username: String,
        name: String,
        surnames: String,
        age: String,
        phone: String,
        status: String,
        provider: String,
        private: Boolean,
        knownPrivateUsers: List<String>,
        qrMark : String
    ) {
        this.uid = uid
        this.username = username
        this.email = email
        this.name = name
        this.surnames = surnames
        this.image = image
        this.search = search
        this.age = age
        this.phone = phone
        this.status = status
        this.provider = provider
        this.private = private
        this.knownPrivateUsers = knownPrivateUsers
        this.qrMark = qrMark
    }

    //getters y setters
    fun getUid() : String?{
        return uid
    }

    fun setUid(uid : String){
        this.uid = uid
    }

    fun getUsername() : String?{
        return username
    }

    fun setUsername(username : String){
        this.username = username
    }

    fun getEmail() : String?{
        return email
    }

    fun setEmail(email : String){
        this.email = email
    }

    fun getImage() : String?{
        return image
    }

    fun setImage(image : String){
        this.image = image
    }

    fun getSearch() : String?{
        return search
    }

    fun setSearch(search : String){
        this.search = search
    }

    fun getSurnames() : String?{
        return surnames
    }

    fun setSurnames(surnames : String){
        this.surnames = surnames
    }

    fun getName() : String?{
        return name
    }

    fun setName(name : String){
        this.name = name
    }

    fun getAge() : String?{
        return age
    }

    fun setAge(age : String){
        this.age = age
    }

    fun getPhone() : String?{
        return phone
    }

    fun setPhone(phone : String){
        this.phone = phone
    }

    fun getStatus() : String?{
        return status
    }

    fun setStatus(status : String){
        this.status = status
    }

    fun getProvider() : String?{
        return provider
    }

    fun setProvider(provider : String){
        this.provider = provider
    }

    fun isPrivate() : Boolean{
        return private
    }

    fun setPrivate(private : Boolean){
        this.private = private
    }

    fun getKnownPrivateUsers() : List<String>{
        return knownPrivateUsers
    }

    fun setKnownPrivateUsers(knownPrivateUsers : List<String>){
        this.knownPrivateUsers = knownPrivateUsers
    }

    fun getQrMark() : String?{
        return qrMark
    }

    fun setQrMark(qrMark : String){
        this.qrMark = qrMark
    }

}