package com.pocketsoap.admin;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

import org.codehaus.jackson.map.ObjectMapper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.*;
import android.os.*;
import android.text.*;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;

import com.pocketsoap.salesforce.*;

/** Activity that is the user detail page, where they can do a reset password, toggle isActive etc */
public class UserDetailActivity extends Activity {

	private static final int RC_CLONE_USER = 42;
	
	static final String EXTRA_USER_JSON = "user_json";

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		helper = new ActivityHelper(this);
        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		try {
			this.user = new ObjectMapper().readValue(getIntent().getStringExtra(EXTRA_USER_JSON), User.class);
			this.salesforce = new SalesforceApi(getIntent());
		} catch (IOException e) {
			helper.showError(e);
		} catch (URISyntaxException e) {
			helper.showError(e);
		}
		setContentView(R.layout.user_detail);
		bindUi();
	}
	
	private ActivityHelper helper;
	private Button resetPasswordButton;
	private CheckBox isActive;
	private SalesforceApi salesforce;
	private User user;

	// take all the data from the User object, and bind into the relevant parts of the UI
	private void bindUi() {
		// header section
		setText(R.id.detail_name, user.Name);
		setText(R.id.detail_username, user.Username);
		setText(R.id.detail_title, user.Title);
		
		// contact section
		setText(R.id.contact_email, user.Email);
		setText(R.id.contact_phone, user.Phone);
		setText(R.id.contact_mobile, user.MobilePhone);

		// no auto link for SMS, so we need to build our own URLSpan for it.
		if (user.MobilePhone != null && user.MobilePhone.length() > 0) {
			SpannableStringBuilder b = new SpannableStringBuilder(user.MobilePhone);
			b.setSpan(new URLSpan("smsto:" + user.MobilePhone), 0, user.MobilePhone.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			setText(R.id.contact_mobile_text, b).setMovementMethod(LinkMovementMethod.getInstance());
		}
		
		//action section
		resetPasswordButton = (Button)findViewById(R.id.detail_reset_pwd);
		isActive = (CheckBox)findViewById(R.id.detail_enabled);
		isActive.setChecked(user.IsActive);
		isActive.setOnClickListener(new ToggleActive());

		// user photo
		// sigh, Android 2.1's SSL handling is not compatible with the way the SSL certs are setup on *.content.force.com
		// so, we can't load the user photo's if we're on 2.1
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1) {
			// the default person image is https://blah/.../005/T but we don't want to bother fetching that, we'll just use our local default instead.
			if (user.SmallPhotoUrl != null && user.SmallPhotoUrl.length() > 0 && !user.SmallPhotoUrl.endsWith("/005/T")) {
				PhotoLoaderTask photoLoader = new PhotoLoaderTask(helper);
				photoLoader.execute(user.SmallPhotoUrl);
			}
		}
	}
	
	private TextView setText(int textId, CharSequence txt) {
		TextView tv = (TextView)findViewById(textId);
		tv.setText(txt);
		return tv;
	}
    
	/** called when the user taps the reset password button */
	public void resetPasswordClicked(View v) {
		ResetPasswordTask t = new ResetPasswordTask(helper);
		t.execute(user.Id);
	}

	/** user tapped the clone button, start the user clone activity */
	public void cloneUserClicked(View v) {
		Intent i = new Intent(this, UserCloneActivity.class);
		i.putExtras(getIntent());
		startActivityForResult(i, RC_CLONE_USER);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == RC_CLONE_USER && resultCode == RESULT_OK)
			finish();
	}

	/** called when the user taps the IsActive checkbox */
	private class ToggleActive implements OnClickListener {
		public void onClick(View v) {
			SetActiveTask t = new SetActiveTask(helper);
			t.execute(!user.IsActive);
		}
	}

	/** background task to toggle the IsActive flag on the User */
	private class SetActiveTask extends ApiAsyncTask<Boolean, Void> {

		SetActiveTask(ActivityCallbacks activity) {
			super(activity);
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			isActive.setEnabled(false);
		}
		
		@Override
		protected Void doApiCall(Boolean... params) throws Exception {
			Map<String, Object> req = new HashMap<String, Object>();
			req.put("IsActive", params[0]);
			salesforce.patchSObjectJson("user", user.Id, req);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			isActive.setEnabled(true);
			super.onPostExecute(result);
		}

		@Override
		protected void handleError(Exception exception) {
			// the Checkbox will of toggled its state automatically
			// but the API change didn't go through so we need to
			// put the checkbox back
			isActive.setChecked(user.IsActive);
			super.handleError(exception);
		}

		@Override
		protected void handleResult(Void result) {
			isActive.setChecked(!user.IsActive);
			user.IsActive = !user.IsActive;
			Toast.makeText(UserDetailActivity.this, getString(R.string.active_updated), Toast.LENGTH_LONG).show();
		}
	}
	
	/** background task to call the ResetPassword API */
	private class ResetPasswordTask extends ApiAsyncTask<String, Void> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			resetPasswordButton.setEnabled(false);
		}

		ResetPasswordTask(ActivityCallbacks activity) {
			super(activity);
		}

		@Override
		protected Void doApiCall(String... params) throws Exception {
			salesforce.resetPassword(params[0]);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			resetPasswordButton.setEnabled(true);
			super.onPostExecute(result);
		}

		@Override
		protected void handleResult(Void result) {
			Toast.makeText(UserDetailActivity.this, getString(R.string.password_was_reset), Toast.LENGTH_LONG).show();
		}
	}
	
	private class PhotoLoaderTask extends ApiAsyncTask<String, Bitmap> {

		PhotoLoaderTask(ActivityCallbacks activity) {
			super(activity);
		}

		@Override
		protected Bitmap doApiCall(String... params) throws Exception {
			byte [] img = salesforce.getBinaryData(params[0]);
			return BitmapFactory.decodeByteArray(img, 0, img.length);
		}

		@Override
		protected void handleResult(Bitmap result) {
			if (result != null) {
				ImageView v = (ImageView)findViewById(R.id.detail_photo);
				v.setImageBitmap(result);
			}
		}
	}
}
