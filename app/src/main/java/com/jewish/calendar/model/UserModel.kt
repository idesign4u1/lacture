package com.jewish.calendar.model

data class UserModel(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val gender: String = "",         // "female" or "male"
    val birthDate: String = "",      // "yyyy-MM-dd" Gregorian, empty if not set
    val maritalStatus: String = ""   // "single", "married", "divorced", "widowed"
) {
    val isFemale: Boolean get() = gender == "female"
}
