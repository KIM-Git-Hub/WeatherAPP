package com.jaeyoung1.weather

import android.app.Application
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.annotations.RealmClass


class RealmDB: Application() {
    override fun onCreate() {
        super.onCreate()

        //초기화
        Realm.init(this)
        //설정
        val config = RealmConfiguration.Builder()
            .allowQueriesOnUiThread(true)
            .allowWritesOnUiThread(true)
            .deleteRealmIfMigrationNeeded()
        //db 갱신할때 삭제 또는 재구축
            .build()
        Realm.setDefaultConfiguration(config)
    }

}