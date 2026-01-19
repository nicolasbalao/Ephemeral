package com.example.ephemeral

import android.content.Context
import com.google.gson.Gson
import java.io.File
import kotlin.uuid.ExperimentalUuidApi

class SessionManager(private val context: Context) {

    private val gson = Gson()
    private val fileName = "sessions.json"

    var currentSession: Session? = null

    private fun getFile(): File = File(context.filesDir, fileName);


    fun loadStore(): SessionsStore {


        val file = getFile()

        if (!file.exists()) {
            return SessionsStore(mutableListOf())
        }

        val json = file.readText()

        return gson.fromJson<SessionsStore>(json, SessionsStore::class.java)
    }

    fun saveStore(store: SessionsStore) {
        val json = gson.toJson(store)
        getFile().writeText(json)
    }


    @OptIn(ExperimentalUuidApi::class)
    fun saveSession(session: Session) {
        val store = loadStore()

        store.sessions.removeAll { it.id == session.id }
        store.sessions.add(session)

        saveStore(store)
    }

    @OptIn(ExperimentalUuidApi::class)
    fun deleteSession(sessionId: String) {
        val store = loadStore()
        store.sessions.removeAll { it.id == sessionId }
        saveStore(store)
    }

    fun deleteAllSession() {
        val store = loadStore()

        store.sessions.clear()
        saveStore(store)
    }

    @OptIn(ExperimentalUuidApi::class)
    fun getSession(sessionId: String): Session? {
        return loadStore().sessions.find { it.id == sessionId }
    }

}