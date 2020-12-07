package com.dakingx.app.dkrecorder.config

import android.content.Context

fun Context.getFileProviderAuthority() = "${packageName}.FILE_PROVIDER"