package com.jc.compute.computers;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.jc.compute.Computer;
import com.jc.compute.ComputersForNamespace.EventType;
import com.jc.compute.Rule;
import com.jc.compute.Snapshot;

public abstract class AbstractComputer<T> implements Computer<T> {

	protected Source _source;
	protected boolean _didFire;
	protected int _violationLevel = 0;
	
	protected ArrayList<Rule<T>> _rules = new ArrayList<Rule<T>>();
			
	public AbstractComputer(Source source) {
		_source = source;
	}
	
	@Override
	public String key() {
		return this.getClass().getSimpleName() + _source.toString();
	}
	
	@Override
	public Source source() {
		return _source;
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
	
	public T computedValue(EventType eventType) {
		if (eventType == EventType.AuditEvent) {
			return computedValueForAuditEvent();
		} else {
			return computedValueForExceptionEvent();
		}
	}
	
	public void clearStickyRule(String ref) {
	
		for (Rule<?> r : this._rules) {
			if (r.description().equals(ref)) {
				r.clear();
				break;
			}
		}
	}
	
	protected EventTypeComputer<T> auditEventTypeComputer() {
		
		
		return new EventTypeComputer<T>() {

			@Override
			public EventType eventType() {
				return EventType.AuditEvent;
			}

			@Override
			public T computedValue() {
				return AbstractComputer.this.computedValueForAuditEvent();
			}

			@Override
			public boolean applyRules(Stack<Snapshot<T>> collatedValues) {
				return AbstractComputer.this.applyRules(collatedValues, EventType.AuditEvent);
			}
		};
	}
	
	protected EventTypeComputer<T> exceptionEventTypeComputer() {
		
		
		return new EventTypeComputer<T>() {

			@Override
			public EventType eventType() {
				return EventType.ExceptionEvent;
			}

			@Override
			public T computedValue() {
				return AbstractComputer.this.computedValueForExceptionEvent();
			}

			@Override
			public boolean applyRules(Stack<Snapshot<T>> collatedValues) {
				return AbstractComputer.this.applyRules(collatedValues, EventType.ExceptionEvent);
			}
		};
	}
	
	protected boolean applyRules(Stack<Snapshot<T>> collatedValues, EventType eventType) {
				
		for (Rule<T> r : _rules) {
			boolean didFire = r.apply(this, collatedValues, eventType);
			
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
	
	protected abstract T computedValueForAuditEvent();
	protected abstract T computedValueForExceptionEvent();

}
