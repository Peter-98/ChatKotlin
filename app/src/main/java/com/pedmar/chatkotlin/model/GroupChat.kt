package com.pedmar.chatkotlin.model

class GroupChat {

    private var uidGroup : String = ""
    private var image : String = ""
    private var name : String = ""
    private var uidUsersList : List<String> = emptyList()
    private var colorUsersList : Map<String, Long> = emptyMap()

    constructor()

    constructor(
        uidGroup: String,
        image: String,
        name: String,
        uidUsersList: List<String>,
        colorUsersList: Map<String, Long>
    ) {
        this.uidGroup = uidGroup
        this.name = name
        this.image = image
        this.uidUsersList = uidUsersList
        this.colorUsersList = colorUsersList
    }

    //getters y setters
    fun getUidGroup() : String?{
        return uidGroup
    }

    fun setUidGroup(uidGroup : String){
        this.uidGroup = uidGroup
    }

    fun getImage() : String?{
        return image
    }

    fun setImage(image : String){
        this.image = image
    }

    fun getName() : String?{
        return name
    }

    fun setName(name : String){
        this.name = name
    }

    fun getUidUsersList() : List<String>?{
        return uidUsersList
    }

    fun setUidUsersList(uidUsersList : List<String>){
        this.uidUsersList = uidUsersList
    }

    fun getColorUsersList() : Map<String, Long>?{
        return colorUsersList
    }

    fun setColorUsersList(colorUsersList : Map<String, Long>){
        this.colorUsersList = colorUsersList
    }
}