package com.minusplayer.app.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionManager {
    enum class Capability { MEDIA_VIDEO, MEDIA_AUDIO, NOTIFICATIONS, LOCAL_NETWORK }

    fun permissionFor(capability: Capability): String? = when (capability) {
        Capability.MEDIA_VIDEO -> if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO else null
        Capability.MEDIA_AUDIO -> if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else null
        Capability.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS else null
        Capability.LOCAL_NETWORK -> if (Build.VERSION.SDK_INT >= 37) Manifest.permission.ACCESS_LOCAL_NETWORK else null
    }

    fun isGranted(context: Context, capability: Capability): Boolean {
        val permission = permissionFor(capability) ?: return true
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun mediaPermissions(): Array<String> = buildList {
        permissionFor(Capability.MEDIA_VIDEO)?.let(::add)
        permissionFor(Capability.MEDIA_AUDIO)?.let(::add)
    }.toTypedArray()
}
