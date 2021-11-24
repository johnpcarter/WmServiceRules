package com.jc.compute;

import java.util.Date;
import java.util.Map;
import java.util.Set;

public class Snapshot<T> {
	
	public Date time;
	public Map<String, T> values;
	public boolean didFireARule = false;
	public Set<String> firedEventTypes;
	public int violationLevel;
	
	Snapshot(Map<String, T> values) {
		this.time = new Date();
		this.values = values;
	}
}
