package net.einsa.lotta

import android.annotation.SuppressLint
import android.content.Context

class App {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var _context: Context? = null
        val context: Context
            get() = _context ?: throw Exception("App context not set")

        private var _mainActivity: MainActivity? = null

        val mainActivity: MainActivity
            get() = _mainActivity ?: throw Exception("MainActivity not set")

        fun set(context: Context) {
            this._context = context
        }

        fun set(mainActivity: MainActivity) {
            this._mainActivity = mainActivity
        }

        fun unset() {
            this._context = null
            this._mainActivity = null
        }
    }
}

