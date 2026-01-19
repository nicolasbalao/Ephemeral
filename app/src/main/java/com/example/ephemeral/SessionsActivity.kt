package com.example.ephemeral

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.uuid.ExperimentalUuidApi

class SessionsActivity : AppCompatActivity() {

    @OptIn(ExperimentalUuidApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_sessions)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val clearBtn = findViewById<Button>(R.id.clear_btn)
        clearBtn.setOnClickListener {
            SessionManager(this).deleteAllSession()
            drawSessionList()
        }
        drawSessionList()
    }


    private fun drawSessionList() {
        val container = findViewById<LinearLayout>(R.id.sessionsContainer)
        container.removeAllViews() // 🔥 LIGNE MANQUANTE

        val sessions = SessionManager(this).loadStore().sessions

        sessions.forEach { session ->
            val button = Button(this).apply {
                text = session.name ?: "Session ${session.id}"
                setOnClickListener {
                    val result = Intent().apply {
                        putExtra("session_id", session.id.toString())
                    }
                    setResult(RESULT_OK, result)
                    finish()
                }
            }
            container.addView(button)
        }
    }


}