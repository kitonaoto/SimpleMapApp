package com.example.mapapp

import android.Manifest
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mapapp.databinding.FragmentRequestPermissionBinding


class RequestPermission: Fragment() {
    private lateinit var binding: FragmentRequestPermissionBinding

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        binding = FragmentRequestPermissionBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 初期で画面開いた時.
        if (isPermitted()) {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        } else {
            binding.message.text = "パーミッションが無効なのでアプリを使用できません."
            // 許可を取得
            requestAllowPermission()
        }
    }

    private fun requestAllowPermission() {
        requestPermissions( arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1000)
    }

    // requestPermissionsで許可ボタンが押された場合
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode != 1000) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        if (grantResults[0] == PERMISSION_GRANTED) {
            // 許可した場合はマップ画面へ遷移.
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
            return
        }
    }

    private fun isPermitted(): Boolean =
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED
}