package net.einsa.lotta

import android.annotation.SuppressLint
import android.content.Context

class App {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var context: Context? = null

        fun get(): Context {
            return context!!
        }

        fun set(context: Context) {
            this.context = context
        }
    }
}

