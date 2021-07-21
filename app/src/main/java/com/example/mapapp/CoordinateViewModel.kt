package com.example.mapapp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CoordinateViewModel: ViewModel() {
    var Coordinate = Coordinate(longitude = null, latitude = null)
    var ShopInfos = MutableLiveData<MutableList<ShopInfo>>(mutableListOf())
}

data class ShopInfo(
        var longitude: Double?,
        var latitude: Double?,
        var shopName: String,
)

data class Coordinate(
    var longitude: Double?,
    var latitude: Double?,
)