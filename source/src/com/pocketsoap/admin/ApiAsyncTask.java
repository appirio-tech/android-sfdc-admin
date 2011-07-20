package com.pocketsoap.admin;

import android.os.AsyncTask;

/** base class for doing background API calls, and updating the UI with results, standardized error handling */
abstract class ApiAsyncTask<ParamType, ResultType> extends AsyncTask<ParamType, Void, ResultType> {

	/** the hosting activity should implement this, so that we can keep it upto date with whats happening */
	interface ActivityCallbacks {
		void setBusy(boolean b);
		void showError(Exception e);
	}
	
	ApiAsyncTask(ActivityCallbacks activity) {
		this.activity = activity;
	}

	protected final ActivityCallbacks activity;
	private Exception exception;
	
	@Override
	protected void onPreExecute() {
		activity.setBusy(true);
	}
	
	@Override
	protected final ResultType doInBackground(ParamType... params) {
		try {
			return doApiCall(params);
		} catch (Exception ex) {
			exception = ex;
		}
		return null;
	}

	protected abstract ResultType doApiCall(ParamType ... params) throws Exception;
	protected abstract void handleResult(ResultType result);
	
	@Override
	protected void onPostExecute(ResultType result) {
		activity.setBusy(false);
		if (exception == null) {
			handleResult(result);
		} else {
			handleError(exception);
		}
	}
	
	protected void handleError(Exception exception) {
		activity.showError(exception);
	}
}