package com.jc.compute.computers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import com.jc.compute.Computer;
import com.jc.compute.Rule;
import com.jc.compute.Snapshot;

public abstract class AbstractComputer<T> implements Computer<T> {

	protected String _key;
	protected String _eventType;
	protected String _namespace;
	protected String _source;
	protected String _uom;
	protected boolean _didFire;
	protected int _violationLevel = 0;
	
	protected ArrayList<Rule<T>> _rules = new ArrayList<Rule<T>>();
	
	protected HashMap<String, T> _running = new HashMap<String, T>();
	
	public AbstractComputer(String source, String uom) {
		_source = source;
		_uom = uom;
	}

	@Override
	public String key() {
		return _key;
	}
	
	@Override
	public String eventType() {
		return _eventType;
	}
	
	@Override
	public String namespace() {
		return _namespace;
	}
	
	@Override
	public void setNamespace(String namespace) {
		this._namespace = namespace;
	}
	
	@Override
	public String source() {
		return _source;
	}
	
	@Override
	public String uom() {
		return _uom;
	}
	
	@Override
	public void addRules(List<Rule<T>> rules) {
		
		for (Rule<T> r : rules) {
			this._rules.add(r);
		}
	}
	
	public void addRule(Rule<T> rule) {
		_rules.add(rule);
	}
	
	public List<Rule<T>> rules() {
		return _rules;
	}
	
	public boolean didFireRule() {
		return _didFire;
	}
	
	public int violationLevel() {
		return _violationLevel;
	}
	
	@Override
	public void add(String id, T value, boolean isStartEvent) {
		
		if (id != null) {
				
			value = getSet(id, value, isStartEvent);
		}
		
		if (value != null) {
			add(value);
		}
	}
	
	@Override
	public boolean applyRules(Stack<Snapshot<T>> collatedValues) {
				
		for (Rule<T> r : _rules) {
			boolean didFire = r.apply(this, collatedValues);
			
			if (didFire) {
				_didFire = true;
				
				// cascade violation level, but only if more important than any existing
				
				if (_violationLevel < r.violationLevel()) {
					_violationLevel = r.violationLevel();
				}
			}
		}
		
		return _didFire;
	}
	
	protected synchronized T getSet(String id, T value, boolean isStartEvent) {
		
		if (_running.get(id) != null) {
			return combined(_running.remove(id), value);
		} else if (isStartEvent) {
			_running.put(id, value);
			return null;
		} else {
			return null;
		}
	}
	
	abstract T combined(T first, T last);
	
}
