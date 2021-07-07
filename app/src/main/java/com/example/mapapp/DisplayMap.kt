package com.example.mapapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.mapapp.databinding.FragmentDisplayMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory


class DisplayMap: Fragment(),
        OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var binding: FragmentDisplayMapBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var scope: CoroutineScope
    private lateinit var locationService: LocationService

    // 現在地の座標
    private var globalLongitude: Double? = null
    private var globalLatitude: Double? = null


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        // retrofit初期化
        initRetrofit()
        // scope初期化
        scope = CoroutineScope(Job() + Dispatchers.Main)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        binding = FragmentDisplayMapBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    private fun isPermitted(): Boolean =
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // もし許可されていない状態でマップの画面へいたら、戻す
//        if (!isPermitted()) {
//            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
//        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        var mapview = binding.map
        mapview.onCreate(savedInstanceState)
        mapview.onResume()
        mapview.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        enableMyLocation()
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        mMap.isMyLocationEnabled = true
        // 現在地ボタンを無効化
        mMap.uiSettings.isMyLocationButtonEnabled = false
        var location = fusedLocationProviderClient.lastLocation
        location.addOnSuccessListener { location: Location? ->
             globalLatitude = location?.latitude
             globalLongitude = location?.longitude
            // カメラをセットする
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location?.latitude!!, location?.longitude!!), 15F))

            // 周囲情報取得
             scope.launch {
                getShopInfo()
             }
        }
    }

    // 半径500mの店舗を取得する
    suspend private fun getShopInfo() {
        val response = locationService.getShopInfo(
                resources.getString(R.string.api_key),
                "${globalLatitude.toString()},${globalLongitude.toString()}",
                500,
                "restaurant"
        )

        if (response.isSuccessful) {
            checkNext(response)
        }
    }

    // next_page_tokenをチェック
    private suspend fun checkNext(response: Response<LocationResponse>) {
        responseLog(response)
        // nextTokenがあれば再度リクエスト
        if (response.body()?.next_page_token != null) {
            Thread.sleep(2000)
            var nextResponse = locationService.getNextShopInfo("${response.body()?.next_page_token}", resources.getString(R.string.api_key))
            if (nextResponse.isSuccessful) {
                checkNext(nextResponse)
            }
        }
    }

    // ショップにマーカする
    private fun responseLog(response: Response<LocationResponse>) {
        var body = response.body()

        body?.results?.forEach {  shop ->
            // Log.d("check", "${shop.name}")
            mMap.addMarker(
                    MarkerOptions()
                            .position(LatLng(shop.geometry.location.lat,shop.geometry.location.lng))
            )
        }
    }

    private fun initRetrofit() {
        locationService = Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com")
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(LocationService::class.java)
    }
}