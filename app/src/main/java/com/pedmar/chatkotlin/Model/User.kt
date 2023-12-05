package com.pedmar.chatkotlin.Model

class User {

    private var uid : String = ""
    private var username : String = ""
    private var email : String = ""
    private var image : String = ""
    private var search : String = ""
    private var alias : String = ""

    constructor()

    constructor(
        uid: String,
        email: String,
        image: String,
        search: String,
        username: String,
        alias: String
    ) {
        this.uid = uid
        this.username = username
        this.email = email
        this.alias = alias
        this.image = image
        this.search = search
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

    fun getAlias() : String?{
        return alias
    }

    fun setAlias(alias : String){
        this.alias = alias
    }
}