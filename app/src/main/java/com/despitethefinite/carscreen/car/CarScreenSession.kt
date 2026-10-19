package com.despitethefinite.carscreen.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class CarScreenSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = ProjectionScreen(carContext)
}
