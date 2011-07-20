package com.pocketsoap.admin;

import com.pocketsoap.salesforce.SalesforceApi;
import com.pocketsoap.salesforce.OAuth2.TokenResponse;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

/** 
 * Common stuff that we need to do from multiple activities. 
 *
 * The Activity class hierarchy (Activity/ListActivity) make it impossible to put this in a base class
 */
public class ActivityHelper implements ApiAsyncTask.ActivityCallbacks {
	
	public ActivityHelper(Activity owner) {
		this(owner, R.string.api_failed);
	}
	
	public ActivityHelper(Activity owner, int errorTextId) {
		this.activity = owner;
		this.errorTextId = errorTextId;
	}
	
	private final Activity activity;
	private final int errorTextId;
	
	public void setBusy(boolean b) {
		activity.setProgressBarIndeterminateVisibility(b);
	}

	public void showError(Exception ex) {
		// Our session expired out from under us. go back to the start.
		if (ex instanceof SalesforceApi.MissingSessionException) {
			Intent i = new Intent(activity, Boot.class);
			i.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivity(i);
			activity.finish();
		}

		Toast.makeText(activity, 
		                activity.getString(errorTextId, ex.getMessage()),
		                Toast.LENGTH_LONG ).show();
    }

	public void startLoginActivity() {
    	Intent i = new Intent(activity, Login.class);
    	activity.startActivity(i);
    	activity.finish();
	}

	public void startUserListActivity(TokenResponse result) {
		startUserListActivity(result.access_token, result.instance_url);
	}
	
	public void startUserListActivity(String sid, String instanceUrl) {
		Intent i = new Intent(activity, UserListActivity.class);
		i.putExtra(SalesforceApi.EXTRA_SID, sid);
		i.putExtra(SalesforceApi.EXTRA_SERVER, instanceUrl);
		activity.startActivity(i);
		activity.finish();
	}
}
