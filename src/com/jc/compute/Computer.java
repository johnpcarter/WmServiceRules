package com.jc.compute;

import java.util.List;
import java.util.Stack;

public interface Computer<T> {
		
	public Computer<T> copy();
	public Computer<T> copy(String key, String event);
	
	public String key();
	public String namespace();
	public String eventType();
	public String source();
	public String uom();
	public int violationLevel();

	public void setNamespace(String namespace);
	
	public void add(T value);
	public void add(String id, T value, boolean isStartEvent);

	public T computedValue();

	public void addRules(List<Rule<T>> rules);

	public List<Rule<T>> rules();
	public boolean didFireRule();
	
	public boolean applyRules(Stack<Snapshot<T>> collatedValues);
}
