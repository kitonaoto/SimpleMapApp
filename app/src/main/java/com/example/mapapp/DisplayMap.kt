package com.example.mapapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.mapapp.databinding.FragmentDisplayMapBinding
import com.google.android.gms.location.*
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

    // ViewModel
    lateinit var viewModel: CoordinateViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // viewModel設定
        activity?.run {
            viewModel = ViewModelProvider(this).get(CoordinateViewModel::class.java)
        }

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
        // 初期動作
        enableMyLocation()
        // 移動検知
        locationChanged()
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        mMap.isMyLocationEnabled = true
        // 現在地ボタンを無効化
        mMap.uiSettings.isMyLocationButtonEnabled = false
        var location = fusedLocationProviderClient.lastLocation
        location.addOnSuccessListener { location: Location? ->
            // viewModelに初期の座標を保存
            viewModel.Coordinate = Coordinate(latitude = location?.latitude, longitude = location?.longitude)

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
                "${viewModel.Coordinate.latitude.toString()},${viewModel.Coordinate.longitude.toString()}",
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

    // 以前いた場所から100m動くと再度周囲情報を取得する
    @SuppressLint("MissingPermission")
    private fun locationChanged() {
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 40 * 1000
        val locationCallback = object: LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult == null) {
                    return
                }
                for (location in locationResult.locations) {
                    if (location != null) {
                        // 以前の座標から100m移動していたら周囲情報を新しく取得し、カメラ移動
                        if (getDistanceBetween(
                                Location(LocationManager.GPS_PROVIDER).apply {
                                    latitude = location.latitude
                                    longitude = location.longitude
                                }
                        ) > 100) {
                            scope.launch {
                                getShopInfo()
                            }
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location?.latitude!!, location?.longitude!!), 15F))
                            //　100m移動した最新の座標で以前の座標を上書き
                            viewModel.Coordinate.latitude = location.latitude
                            viewModel.Coordinate.longitude = location.longitude
                        }
                    }
                }
            }
        }
        // 移動検知
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper())
    }

    // ２点間距離取得
    private fun getDistanceBetween(locationX: Location): Float {
        return locationX.distanceTo(Location(LocationManager.GPS_PROVIDER).apply {
            latitude = viewModel.Coordinate.latitude!!
            longitude = viewModel.Coordinate.longitude!!
        })
    }
}