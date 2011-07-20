package com.pocketsoap.admin;

import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;

import com.pocketsoap.salesforce.*;
import com.pocketsoap.salesforce.OAuth2.TokenResponse;

/** This is the Boot/Loader activity it, checks for a saved refresh token, generates a sid, or if that fails, starts the oauth flow */
public class Boot extends Activity {

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		helper = new BootActivityHelper(this);
        tokenStore = new RefreshTokenStore(this);
        if (!tokenStore.hasSavedToken()) {
        	// no refresh token stored, go straight to login.
        	helper.startLoginActivity();
        }
        // otherwise show the boot screen while we validate the refresh token
        setContentView(R.layout.boot);
    }

	private ActivityHelper helper;
    private RefreshTokenStore tokenStore;
	
	@Override
	public void onResume() {
		super.onResume();
		// start validating the cached ref token.
		TokenRefresherTask tr = new TokenRefresherTask(helper);
		tr.execute(tokenStore.getRefreshToken(), tokenStore.getAuthServer());
	}

	/** background task that calls the OAuth Token service to get a new access token using the refresh token */
	private class TokenRefresherTask extends ApiAsyncTask<String, TokenResponse> {

		TokenRefresherTask(ActivityCallbacks activity) {
			super(activity);
		}

		@Override
		protected TokenResponse doApiCall(String... params) throws Exception {
			return new OAuth2().refreshToken(params[0], params[1]);
		}

		@Override
		protected void handleResult(TokenResponse result) {
			if (result.error != null) {
				activity.showError(new IOException(result.error_description));
			} else {
				helper.startUserListActivity(result);
			}
		}
	}

	private static class BootActivityHelper extends ActivityHelper {

		BootActivityHelper(Activity a) {
			super(a, R.string.auth_failed);
		}

		@Override
		public void showError(Exception ex) {
			super.showError(ex);
			startLoginActivity();
		}
	}
}
