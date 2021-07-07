package com.example.mapapp

import androidx.lifecycle.ViewModel

class CoordinateViewModel: ViewModel() {
    var Coordinate = Coordinate(longitude = null, latitude = null)
}

data class Coordinate(
    var longitude: Double?,
    var latitude: Double?,
)