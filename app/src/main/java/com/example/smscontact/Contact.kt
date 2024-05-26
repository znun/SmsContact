package com.example.smscontact

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Contact(val name: String, val number: String) {
    companion object {
        fun fromJson(json: String?): MutableList<Contact> {
            if (json == null) return mutableListOf()
            val type = object : TypeToken<MutableList<Contact>>() {}.type
            return Gson().fromJson(json, type)
        }

        fun toJson(contacts: List<Contact>): String {
            return Gson().toJson(contacts)
        }
    }
}
