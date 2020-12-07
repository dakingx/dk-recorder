package com.dakingx.app.dkrecorder

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.dakingx.app.dkrecorder.config.getFileProviderAuthority
import com.dakingx.dkrecorder.fragment.RecorderFragment
import com.dakingx.dkrecorder.fragment.TinyRecorderFragment
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        addRecorderFragment()
    }

    private fun toast(stringResId: Int) {
        Toast.makeText(this, getString(stringResId), Toast.LENGTH_SHORT).show()
    }

    private fun addRecorderFragment() {
        Dexter.withContext(this)
            .withPermissions(*RecorderFragment.REQUIRED_PERMISSIONS.toTypedArray())
            .withListener(object :
                MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        runOnUiThread {
                            val fragment =
                                TinyRecorderFragment.newInstance(getFileProviderAuthority(), 8)
                            supportFragmentManager.beginTransaction()
                                .add(R.id.container_recorder, fragment)
                                .commitNow()
                        }
                    } else {
                        runOnUiThread {
                            toast(R.string.tip_lack_required_permissions)
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    list: MutableList<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()
    }
}