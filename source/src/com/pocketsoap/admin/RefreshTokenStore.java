package com.pocketsoap.admin;

import android.content.*;
import android.content.SharedPreferences.Editor;

/** helper for reading/writing the refresh token from the preferences */
public class RefreshTokenStore {

	RefreshTokenStore(Context ctx) {
		this.pref = ctx.getSharedPreferences("a", Context.MODE_PRIVATE);
	}
	
	private final SharedPreferences pref;
	
	void saveToken(String refreshToken, String authServer) {
		Editor e = pref.edit();
		e.putString(REF_TOKEN, refreshToken);
		e.putString(AUTH_SERVER, authServer);
		e.commit();
	}
	
	void clearSavedData() {
		Editor e = pref.edit();
		e.clear();
		e.commit();
	}
	
	boolean hasSavedToken() {
		return pref.contains(REF_TOKEN);
	}
	
	String getRefreshToken() {
		return pref.getString(REF_TOKEN, null);
	}
	
	String getAuthServer() {
		return pref.getString(AUTH_SERVER, Login.PROD_AUTH_HOST);
	}
	
	
    private static final String REF_TOKEN = "refTKn";
	private static final String AUTH_SERVER = "authServer";
}
