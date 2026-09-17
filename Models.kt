package pk.shajranasab.model

data class Person(
    val id:String="", val name:String="", val fatherName:String="", val motherName:String="",
    val birthDate:String="", val deathDate:String="", val spouse:String="", val notes:String="",
    val parentId:String="", val createdBy:String="", val updatedBy:String=""
)
data class UserProfile(
    val uid:String="", val name:String="", val email:String="", val role:String="viewer",
    val approved:Boolean=false, val pinHash:String="", val fcmToken:String=""
)
data class Announcement(
    val id:String="", val title:String="", val body:String="", val pinned:Boolean=false,
    val createdBy:String="", val createdAt:Long=0L
)
data class AuditLog(
    val id:String="", val action:String="", val personId:String="", val personName:String="",
    val actorUid:String="", val actorName:String="", val timestamp:Long=0L,
    val before:Map<String,Any?>=emptyMap(), val after:Map<String,Any?>=emptyMap()
)
