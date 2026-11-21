package com.soulknight.matrixai.macro

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MacroConfigStoreTest {
    private lateinit var context: Context
    private lateinit var store: MacroConfigStore

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("matrix_macro_settings", Context.MODE_PRIVATE)
            .edit().clear().commit()
        store = MacroConfigStore(context)
    }

    @Test
    fun namedProfileCanBeSavedLoadedAndDeleted() {
        val expected = MacroConfig(
            attackEnabled = false,
            attackIntervalMs = 700L,
            skillEnabled = true,
            skillIntervalMs = 4_000L,
            switchWeaponEnabled = true,
            switchWeaponIntervalMs = 8_000L
        )

        store.saveProfile("Boss", expected)

        assertEquals(expected, store.loadProfile("Boss"))
        assertTrue(store.profileNames().contains("Boss"))
        assertTrue(store.deleteProfile("Boss"))
        assertFalse(store.profileNames().contains("Boss"))
        assertEquals(MacroConfig(), store.loadProfile("Boss"))
    }

    @Test
    fun legacyDefaultSettingsRemainReadable() {
        val expected = MacroConfig(attackIntervalMs = 350L, skillEnabled = false)
        store.save(expected)

        assertEquals(expected, store.load())
        assertEquals(expected, store.loadProfile(MacroConfigStore.DEFAULT_PROFILE))
    }
}
