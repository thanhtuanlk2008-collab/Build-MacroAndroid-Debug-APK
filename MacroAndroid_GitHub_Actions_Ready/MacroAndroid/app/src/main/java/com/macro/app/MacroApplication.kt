package com.macro.app

import android.app.Application
import com.macro.app.data.MacroDatabase
import com.macro.app.data.MacroRepository
import com.macro.app.data.SettingsDataStore
import com.macro.app.engine.MacroEngine
import com.macro.app.input.ShizukuProbeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MacroApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: MacroDatabase
        private set
    lateinit var repository: MacroRepository
        private set
    lateinit var settingsDataStore: SettingsDataStore
        private set

    override fun onCreate() {
        super.onCreate()
        MacroEngine.init(this)
        ShizukuProbeManager.initialize(this)
        database = MacroDatabase.getInstance(this)
        repository = MacroRepository(database.macroDao())
        settingsDataStore = SettingsDataStore(this)

        appScope.launch {
            repository.seedStarterMacroIfEmpty()
        }
    }
}
