package com.jc.compute;

import java.util.List;
import java.util.Stack;

import com.jc.compute.ComputersForNamespace.EventType;

public interface Computer<T> {

	public enum Source {
		Count,
		Duration,
		Transaction
	}
	
	public interface EventTypeComputer<T> {
		
		public EventType eventType();
		public T computedValue();
		public boolean applyRules(Stack<Snapshot<T>> collatedValues);
	}
	
	public Computer<T> copy();
		
	public String key();
	public int type();
	public Source source();
	public int violationLevel();	

	public void addRules(List<Rule<T>> rules);

	public List<Rule<T>> rules();
	public boolean didFireRule();
	public void clearStickyRule(String ref);

	public boolean includesEventType(EventType eventType);
	public void addEventType(EventType eventType);
	
	public void record(EventType eventType, Number value);

	public T computedValue(EventType eventType);
	//public boolean applyRules(Stack<Snapshot<T>> collatedValues);

	public EventTypeComputer<T>[] eventTypes();	
}
