package com.greatideasoft.cwm.app.ui

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.greatideasoft.cwm.domain.user.data.UserItem
import com.greatideasoft.cwm.app.R
import com.greatideasoft.cwm.app.core.log.logInfo
import com.greatideasoft.cwm.app.core.permissions.AppPermission
import com.greatideasoft.cwm.app.core.permissions.handlePermission
import com.greatideasoft.cwm.app.core.permissions.onRequestPermissionsResultReceived
import com.greatideasoft.cwm.app.core.permissions.requestAppPermissions
import com.greatideasoft.cwm.app.databinding.ActivityMainBinding
import com.greatideasoft.cwm.app.utils.extensions.showErrorDialog
import com.greatideasoft.cwm.domain.user.data.BaseUserInfo
import com.greatideasoft.cwm.domain.user.data.Gender
import com.greatideasoft.cwm.domain.user.data.LocationPoint
import com.greatideasoft.cwm.domain.user.data.SelectionPreferences
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.greatideasoft.cwm.domain.photo.PhotoItem
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity: AppCompatActivity() {
	
	companion object {
		private const val TAG = "mylogs_MainActivity"
		
		var currentUser: UserItem? = null
			@Synchronized set
		
		private const val PROVIDER_COARSE = LocationManager.NETWORK_PROVIDER
		private const val PROVIDER_FINE = LocationManager.GPS_PROVIDER
	}
	private val providersList = arrayOf(PROVIDER_COARSE, PROVIDER_FINE)
	private var isStarted = false
	
	private val mLocationManager: LocationManager by lazy {
		getSystemService(Context.LOCATION_SERVICE) as LocationManager
	}
	private val sharedViewModel: SharedViewModel by viewModels()
	
	private val navController by lazy {
		findNavController(R.id.flowHostFragment)
	}

	override fun onCreate(savedInstanceState: Bundle?) {

		WindowCompat.setDecorFitsSystemWindows(
			window.apply { addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS) },
			false
		)
		
		super.onCreate(savedInstanceState)

		DataBindingUtil.setContentView(this, R.layout.activity_main) as ActivityMainBinding
		
		observeUser()
		sharedViewModel.showErrorDialog(this, this)
	}
	
	override fun onStart() {
		if (!isStarted) {
			if (!hasLocationEnabled()) showLocationIsNotEnabled()
			else handleLocationPermission()
		}
		super.onStart()
	}
	
	private fun observeUser() = sharedViewModel.userState.observe(this) { userState ->
		userState.fold(authenticated = {
			logInfo(TAG, "$it")
			currentUser = it
			navController.navigate(R.id.action_global_mainFlowFragment)
			
			val testLat = 38.7604408
			val testLon = 30.539412
			val geohash = GeoFireUtils.getGeoHashForLocation(GeoLocation(testLat, testLon))
			
			val maleNames = listOf("Ahmet", "Mehmet", "Can", "Murat", "Burak", "Emre", "Ozan", "Yiğit")
			val femaleNames = listOf("Ayşe", "Fatma", "Merve", "Zeynep", "Ece", "Selin", "Deniz", "Gözde")
			
			for (i in 1..10) {
				val isMale = i <= 5
				val randomName = if (isMale) maleNames.random() + " " + (100..999).random() 
				                 else femaleNames.random() + " " + (100..999).random()
				
				val testUser = UserItem(
					baseUserInfo = BaseUserInfo(
						name = randomName,
						age = (18..40).random(),
						gender = if (isMale) Gender.MALE else Gender.FEMALE,
						userId = "test_user_faker_$i",
						mainPhotoUrl = "https://i.pravatar.cc/300?u=test_user_faker_$i"
					),
					photoURLs = listOf(PhotoItem(fileName = "photo_$i", fileUrl = "https://i.pravatar.cc/300?u=test_user_faker_$i")),
					location = LocationPoint(testLat, testLon, geohash),
					preferences = SelectionPreferences(
						gender = if (isMale) SelectionPreferences.PreferredGender.FEMALE else SelectionPreferences.PreferredGender.MALE,
						radius = 100.0
					)
				)
				sharedViewModel.updateUser(testUser)
			}
		}, unauthenticated = {
			currentUser = null
			navController.navigate(R.id.action_global_authFragment)
		}, unregistered = {
			currentUser = null
			sharedViewModel.userInfoForRegistration.postValue(it)
			navController.navigate(R.id.action_global_registrationFragment)
		})
	}
	
	private fun hasLocationEnabled(): Boolean = providersList.any { hasLocationEnabled(it) }
	
	private fun hasLocationEnabled(providerName: String): Boolean = try {
		mLocationManager.isProviderEnabled(providerName)
	} catch (e: Exception) {
		false
	}
	
	private fun openSettings() = startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
	
	private fun handleLocationPermission() = handlePermission(
		AppPermission.LOCATION,
		onGranted = { sharedViewModel.listenUserFlow() },
		onDenied = { requestAppPermissions(it) },
		onExplanationNeeded = { it.explanationMessageId }
	).also { isStarted = true }
	
	private fun showLocationIsNotEnabled() = MaterialAlertDialogBuilder(this)
		.setTitle(R.string.dialog_location_disabled_title)
		.setMessage(R.string.dialog_location_disabled_message)
		.setPositiveButton(R.string.dialog_location_btn_pos) { _, _ -> openSettings() }
		.setNegativeButton(R.string.dialog_location_btn_neg, null)
		.create()
		.show()
	
	
	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		onRequestPermissionsResultReceived(
			requestCode,
			grantResults,
			onPermissionGranted = { if (it == AppPermission.LOCATION) sharedViewModel.listenUserFlow() },
			onPermissionDenied = { it.deniedMessageId })
	}
	
}
