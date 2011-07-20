package com.pocketsoap.salesforce;

import java.util.List;

/** the results of a query that returns User rows */
public class UserQueryResult {
	public int totalSize;
	public boolean done;
	public List<User> records;
}