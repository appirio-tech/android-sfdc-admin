package com.pocketsoap.admin;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;

import android.app.ListActivity;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.widget.TextView.OnEditorActionListener;

import com.pocketsoap.salesforce.*;

/** the user list, this defaults to showing the recent users, and allows for a search */
public class UserListActivity extends ListActivity implements OnEditorActionListener {

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		helper = new ActivityHelper(this);
        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.user_list);
		search = (EditText)findViewById(R.id.search_text);
		search.setOnEditorActionListener(this);
		emptyText = (TextView)findViewById(android.R.id.empty);
		
		// inflate the view for the header view, and add it to the list view
		View header = ((LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.list_header, null);
		listHeaderText = (TextView)header.findViewById(R.id.list_header_text);
		getListView().addHeaderView(header, null, false);
	}

	private ActivityHelper helper;
	private SalesforceApi salesforce;
	private EditText search;
	private TextView emptyText;
	private TextView listHeaderText;

	@Override
	public void onResume() {
		super.onResume();
		try {
			salesforce = new SalesforceApi(getIntent());
			emptyText.setText(getString(R.string.loading));
			startFetchUsers(search.getText().toString());
		} catch (URISyntaxException e) {
			helper.showError(e);
		}
		TextView f = (TextView)findViewById(R.id.footer_text);
		f.setText(salesforce.getInstanceUri().getHost());
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	switch (item.getItemId()) {
    		case R.id.menu_logout:
    			new RefreshTokenStore(this).clearSavedData();
    			helper.startLoginActivity();
    			finish();
    	}
    	return super.onMenuItemSelected(featureId, item);
    }
    
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// the user tapped a row in the list, serialize up the data for that row, and star the detail page activity
		Intent d = new Intent(this, UserDetailActivity.class);
		d.putExtras(getIntent());
		try {
			d.putExtra(UserDetailActivity.EXTRA_USER_JSON, new ObjectMapper().writeValueAsString(v.getTag()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		startActivity(d);
	}

	// build an adapter for this list of users, so they can be rendered in the list view.
	private <T> void bindUserList(List<User> users) {
		UserListAdapter a = new UserListAdapter(this, R.layout.user_row, users);
		this.setListAdapter(a);
		if (users.size() == 0)
			emptyText.setText(getString(R.string.no_results));
	}
	
	// Adapter/Binder that renders the list view rows.
	private class UserListAdapter extends ArrayAdapter<User> {

		public UserListAdapter(Context context, int textViewResourceId, List<User> objects) {
			super(context, textViewResourceId, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null)
				convertView = ((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.user_row, null);
			User u = getItem(position);
			setText(convertView, R.id.user_name, u.Name);
			setText(convertView, R.id.user_username, u.Username);
			setText(convertView, R.id.user_title, u.Title);
			convertView.setTag(u);
			return convertView;
		}
		
		private void setText(View v, int textViewId, String text) {
			TextView tv = (TextView)v.findViewById(textViewId);
			tv.setText(text);
		}
	}
	
	/** returns true if we should do a search, or false if we should show the recent list */
	private boolean shouldDoSearch(String searchTerm) {
		return searchTerm != null && searchTerm.trim().length() > 0;
	}
	
	/** background task to run the search query, and bind the results to the UI */
	private class UserSearchTask extends ApiAsyncTask<String, List<User>> {

		UserSearchTask(ActivityCallbacks activity) {
			super(activity);
		}
		
		@Override
		protected List<User> doApiCall(String... params) throws Exception {
			String search = params[0];
			if (shouldDoSearch(search)) 
				return salesforce.userSearch(search, 25);
			return salesforce.getRecentUsers();
		}

		@Override
		protected void handleResult(List<User> result) {
			bindUserList(result);
		}
	}
	
	protected void startFetchUsers(String searchTerm) {
		int txtId = shouldDoSearch(searchTerm) ? R.string.list_search_results : R.string.list_recent;
		listHeaderText.setText(getString(txtId));
		UserSearchTask t = new UserSearchTask(helper);
		t.execute(searchTerm);
	}
	
	/** called when the user clicks the search button or enter on the keyboard */
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		startFetchUsers(v.getText().toString());
		return true;
	}
}
