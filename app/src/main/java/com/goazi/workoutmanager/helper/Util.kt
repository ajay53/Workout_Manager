package com.goazi.workoutmanager.helper

import android.view.View
import com.google.android.material.snackbar.Snackbar

class Util {
    companion object {
        private const val TAG = "Util"

        fun showSnackBar(view: View, message: String) {
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
        }
    }
}