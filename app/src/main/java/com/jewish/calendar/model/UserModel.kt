package com.jewish.calendar.model

data class UserModel(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val gender: String = ""  // "female" or "male"
) {
    val isFemale: Boolean get() = gender == "female"
}
