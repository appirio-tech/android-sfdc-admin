package com.pocketsoap.admin;

import java.io.IOException;
import java.net.*;
import java.util.*;

import org.codehaus.jackson.map.ObjectMapper;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import com.pocketsoap.salesforce.*;

/** lets the user set the username/email/first/last for the created cloned user */
public class UserCloneActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		helper = new ActivityHelper(this);
        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		try {
			this.user = new ObjectMapper().readValue(getIntent().getStringExtra(UserDetailActivity.EXTRA_USER_JSON), User.class);
			this.salesforce = new SalesforceApi(getIntent());
		} catch (IOException e) {
			helper.showError(e);
		} catch (URISyntaxException e) {
			helper.showError(e);
		}
		setContentView(R.layout.clone);
		bindUi();
		setResult(RESULT_CANCELED);
	}
	

	private ActivityHelper helper;
	private SalesforceApi salesforce;
	private User user;
	private Button createButton;
	
	private EditText username, firstname, lastname, email;
	
	private void bindUi() {
		createButton = (Button)findViewById(R.id.create_user_button);
		username = (EditText)findViewById(R.id.clone_username);
		firstname = (EditText)findViewById(R.id.clone_firstname);
		lastname = (EditText)findViewById(R.id.clone_lastname);
		email = (EditText)findViewById(R.id.clone_email);
		
		firstname.setText(user.FirstName);
		lastname.setText(user.LastName);
		email.setText(afterAt(user.Email));
		username.setText(afterAt(user.Username));
	}
	
	// converts foo@bar.com to @bar.com
	private String afterAt(String s) {
		int a = s.lastIndexOf('@');
		if (a == -1) return s;
		return s.substring(a);
	}
	
	public void createUserClicked(View v) {
		User newUser = new User();
		newUser.attributes = user.attributes;
		newUser.Username = username.getText().toString().trim();
		newUser.FirstName = firstname.getText().toString().trim();
		newUser.LastName = lastname.getText().toString().trim();
		newUser.Email = email.getText().toString().trim();
		
		if (newUser.Username.length() == 0 || newUser.LastName.length() == 0 || newUser.Email.length() == 0) {
			Toast.makeText(this, getString(R.string.clone_validation_err), Toast.LENGTH_LONG).show();
			return;
		}
		
		CloneUserTask t = new CloneUserTask(helper);
		t.execute(newUser);
	}
	
	private class CloneUserTask extends ApiAsyncTask<User, String> {

		CloneUserTask(ActivityCallbacks a) {
			super(a);
		}
		
		@Override
		protected void onPreExecute() {
			createButton.setEnabled(false);
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(String result) {
			createButton.setEnabled(true);
			super.onPostExecute(result);
		}

		@Override
		@SuppressWarnings("unchecked")
		protected String doApiCall(User ... params) throws Exception {
			User newUser = params[0];
			URI userToCloneUri = new URI(newUser.attributes.url);
			Map<String, Object> userProps = salesforce.getJson(userToCloneUri, Map.class);
			// update properties with the ones set by the user
			userProps.put("Username", newUser.Username);
			userProps.put("FirstName", newUser.FirstName);
			userProps.put("LastName", newUser.LastName);
			userProps.put("Email", newUser.Email);
			// remove some fields that aren't valid for create.
			userProps.remove("CommunityNickname");
			userProps.remove("attributes");
			userProps.remove("Id");

			// use the user describe to remove any other fields that aren't valid for create.
			Map<String, Object> desc = salesforce.getJson(new URI("sobjects/user/describe"), Map.class);
			List<Map<String, Object>> fields = (List<Map<String, Object>>)desc.get("fields");
			for (Map<String, Object> fieldProps : fields) {
				if ((Boolean)fieldProps.get("createable")) continue;
				userProps.remove(fieldProps.get("name"));
			}
			// try and create the new user
			SaveResult sr = salesforce.createSObject("user", userProps);
			if (!sr.success)
				throw new IOException(sr.errors.get(0).message);
			// fetch the new record to update the mru
			salesforce.getJson(salesforce.getRestRootUri().resolve("sobjects/user/" + sr.id + "?fields=Id"), Map.class);
			return sr.id;
		}
		
		@Override
		protected void handleResult(String res) {
			Toast.makeText(UserCloneActivity.this,
					getString(R.string.clone_done),
					Toast.LENGTH_LONG).show();
			setResult(RESULT_OK);
			finish();
		}
	}
}
