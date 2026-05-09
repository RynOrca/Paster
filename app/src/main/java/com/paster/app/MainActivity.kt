package com.paster.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var toggleServiceBtn: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createLayout()
        updateUI()

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun createLayout() {
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding * 2, padding, padding)
        }

        rootLayout.addView(TextView(this).apply {
            text = "Paster"
            textSize = 28f
            setTextColor(0xFF4CAF50.toInt())
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = (8 * resources.displayMetrics.density).toInt()
            layoutParams = lp
        })

        rootLayout.addView(TextView(this).apply {
            text = "点击悬浮球 → 自动复制相册最新图片到剪贴板"
            textSize = 14f
            setTextColor(0xFF666666.toInt())
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = (24 * resources.displayMetrics.density).toInt()
            layoutParams = lp
        })

        statusText = TextView(this).apply {
            textSize = 18f
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = (16 * resources.displayMetrics.density).toInt()
            layoutParams = lp
        }
        rootLayout.addView(statusText)

        toggleServiceBtn = MaterialButton(this).apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = (32 * resources.displayMetrics.density).toInt()
            layoutParams = lp
            setOnClickListener { toggleService() }
        }
        rootLayout.addView(toggleServiceBtn)

        addGuideItem(rootLayout, "悬浮窗权限（必需）",
            "前往 设置 → 应用管理 → Paster → 开启「显示悬浮窗」"
        ) { openOverlaySettings() }

        addGuideItem(rootLayout, "存储权限（必需）",
            "允许 Paster 读取相册中的截图"
        ) { requestStoragePermission() }

        addGuideItem(rootLayout, "开机自启动（推荐）",
            "前往 设置 → 应用管理 → Paster → 开启「自启动」"
        ) { openAppSettings() }

        addGuideItem(rootLayout, "后台运行（推荐）",
            "前往 设置 → 电池 → 高耗电管理 → 允许 Paster 后台运行"
        ) { openAppSettings() }

        rootLayout.addView(TextView(this).apply {
            text = "使用方法：系统截图（电源+音量下）→ 点击悬浮球 → 已复制到剪贴板"
            textSize = 13f
            setTextColor(0xFF999999.toInt())
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = (16 * resources.displayMetrics.density).toInt()
            layoutParams = lp
        })

        setContentView(ScrollView(this).apply { addView(rootLayout) })
    }

    private fun addGuideItem(parent: LinearLayout, title: String, desc: String, action: () -> Unit) {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = (14 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
            setBackgroundColor(0x0A000000.toInt())
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, 0, (8 * resources.displayMetrics.density).toInt())
            layoutParams = lp
        }
        item.addView(TextView(this).apply {
            text = title; textSize = 15f; setTextColor(0xFF333333.toInt())
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = (4 * resources.displayMetrics.density).toInt()
            layoutParams = lp
        })
        item.addView(TextView(this).apply {
            text = desc; textSize = 13f; setTextColor(0xFF888888.toInt())
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = (8 * resources.displayMetrics.density).toInt()
            layoutParams = lp
        })
        item.addView(Button(this).apply {
            text = getString(R.string.go_to_settings); textSize = 12f
            setOnClickListener { action() }
        })
        parent.addView(item)
    }

    private fun updateUI() {
        val canOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true

        val hasStorage = hasStoragePermission()

        when {
            !canOverlay -> {
                statusText.text = "○ 需要悬浮窗权限"
                statusText.setTextColor(0xFFFF9800.toInt())
                toggleServiceBtn.text = "开启悬浮窗权限"
            }
            !hasStorage -> {
                statusText.text = "○ 需要存储权限"
                statusText.setTextColor(0xFFFF9800.toInt())
                toggleServiceBtn.text = "授予存储权限"
            }
            else -> {
                statusText.text = "● 悬浮球已就绪"
                statusText.setTextColor(0xFF4CAF50.toInt())
                toggleServiceBtn.text = "显示悬浮球"
            }
        }
    }

    private fun toggleService() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(this) -> openOverlaySettings()
            !hasStoragePermission() -> requestStoragePermission()
            else -> {
                FloatingBallService.start(this)
                updateUI()
            }
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES), 101)
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 101)
        }
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        })
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
            } else {
                openAppSettings()
            }
        }
    }
}
