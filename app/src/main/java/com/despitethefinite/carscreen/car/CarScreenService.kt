package com.despitethefinite.carscreen.car

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/** Entry point Android Auto binds to. */
class CarScreenService : CarAppService() {

    // This app is sideloaded, never Play-distributed, so accept any host (Android Auto, DHU).
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = CarScreenSession()
}
