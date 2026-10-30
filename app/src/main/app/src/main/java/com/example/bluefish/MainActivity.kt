package com.example.bluefish

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        findViewById<Button>(R.id.btnStart).setOnClickListener { startPet() }
        findViewById<Button>(R.id.btnStop).setOnClickListener {
            stopService(Intent(this, FishService::class.java))
            Toast.makeText(this, "大肥鱼回家啦～", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnGuide).setOnClickListener { showGuide() }
    }

    private fun startPet() {
        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("需要悬浮窗权限")
                .setMessage("请允许「蓝色大肥鱼」显示在其他应用的上层，否则桌宠无法出现。")
                .setPositiveButton("去设置") { _, _ ->
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                    )
                }
                .setNegativeButton("取消", null)
                .show()
            return
        }

        val intent = Intent(this, FishService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "大肥鱼来啦！", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun showGuide() {
        AlertDialog.Builder(this)
            .setTitle("红米 / 小米 设置指引")
            .setMessage(
                "1. 权限管理 → 悬浮窗 → 允许\n" +
                "2. 权限管理 → 通知 → 允许\n" +
                "3. 省电策略 → 无限制\n" +
                "4. 自启动 → 允许\n\n" +
                "如果桌面图标点开没反应，去「设置 → 应用设置 → 应用管理」里找到它，手动授予「显示在其他应用上层」。"
            )
            .setPositiveButton("知道了", null)
            .show()
    }
}
