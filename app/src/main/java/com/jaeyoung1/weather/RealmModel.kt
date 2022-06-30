package com.jaeyoung1.weather

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required


open class RealmModel: RealmObject() {
    @PrimaryKey
    var id: Int = 0
    @Required
    var text: String = ""
}