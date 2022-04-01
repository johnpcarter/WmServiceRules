package com.jc.compute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import com.jc.compute.Computer.Source;
import com.jc.compute.ComputersForNamespace.EventType;

public class Snapshot<T> {
	
	public Date time;
	public boolean didFireARule = false;
	public Set<EventType> firedEventTypes;
	public int violationLevel;
	
	public Collection<Value> values;

	Snapshot() {
		this.time = new Date();
		this.values = new ArrayList<Value>();
	}
	
	void add(EventType eventType, Source source, int type, T value) {
		
		((ArrayList<Value>) this.values).add(new Value(eventType, source, type, value));
	}
	
	public class Value {
		public EventType eventType;
		public Source source;
		public int type;
		
		public T value;
		
		protected Value(EventType eventType, Source source, int type, T value) {
			this.eventType = eventType;
			this.source = source;
			this.type = type;
			this.value = value;
		}
	}
}
