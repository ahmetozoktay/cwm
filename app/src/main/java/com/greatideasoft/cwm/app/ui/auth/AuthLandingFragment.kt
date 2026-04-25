package com.greatideasoft.cwm.app.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.greatideasoft.cwm.app.R
import com.greatideasoft.cwm.app.databinding.FragmentAuthLandingBinding
import com.greatideasoft.cwm.app.ui.common.base.BaseFragment
import com.greatideasoft.cwm.app.utils.extensions.showToastText
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthLandingFragment: BaseFragment<AuthViewModel, FragmentAuthLandingBinding>(
	layoutId = R.layout.fragment_auth_landing
) {
	
	override val mViewModel: AuthViewModel by viewModels()
	
	private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
		val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
		try {
			val account = task.getResult(ApiException::class.java)!!
			mViewModel.signIn(account.idToken!!)
		} catch (e: ApiException) {
			android.util.Log.e("AuthLandingFragment", "Google sign in failed code: ${e.statusCode}, message: ${e.message}")
			view?.context?.showToastText("Google sign in failed: ${e.message} (Code: ${e.statusCode})")
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) = binding.run {
		btnGoogleLogin.setOnClickListener {
			val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestIdToken(getString(R.string.default_web_client_id))
				.requestEmail()
				.build()

			val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
			googleSignInLauncher.launch(googleSignInClient.signInIntent)
		}

		tvOpenPolicies.setOnClickListener {
			var url = getString(R.string.privacy_policy_url)
			if (!url.startsWith("http://") && !url.startsWith("https://"))
				url = "http://$url"
			val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
			startActivity(browserIntent)
		}
	}

}
