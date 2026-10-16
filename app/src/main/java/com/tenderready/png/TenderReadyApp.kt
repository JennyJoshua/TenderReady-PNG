package com.tenderready.png

import android.app.Application
import com.tenderready.png.data.AppDatabase
import com.tenderready.png.data.AppRepository

class TenderReadyApp : Application() {
    val repository: AppRepository by lazy { AppRepository(AppDatabase.build(this)) }
}
