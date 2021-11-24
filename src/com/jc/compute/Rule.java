package com.jc.compute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

public abstract class Rule<T> {

	protected String _eventType;
	protected String _alertType;
	private int _minOccurrences = 1;
	private boolean _fired = false;

	protected RuleAction<T> _action;
	protected boolean _sticky = false;
	protected int _level = 0;
	protected boolean _sendEmail = false;
	
	public Rule(String eventType, RuleAction<T> action, int minOccurrences, boolean isSticky, int level, boolean sendEmail) {
		
		_eventType = eventType;
		_action = action;
		_minOccurrences = minOccurrences;
		_sticky = isSticky;
		_level = level;
		_sendEmail = sendEmail;
		_alertType = this.getClass().getSimpleName();
	}
	
	public abstract Rule<T> clone();
	
	public RuleAction<T> getAction() {
		return _action;
	}
	
	public int minOccurences() {
		return _minOccurrences;
	}
	
	public boolean didFire() {
		return _fired;
	}
	
	public int violationLevel() {
		return _level;
	}
	
	public boolean apply(Computer<T> computer, Stack<Snapshot<T>> collatedValues) {
		
		String message = null;
		
		if (allow(computer)) {
			if ((message=applyRule(computer, allValuesCollectionInterval(collatedValues))) != null) {
				
				Snapshot<T> last = collatedValues.lastElement();
				
				last.violationLevel = this._level;
				
				if (_minOccurrences <= 1 || doOccurrencesExceedMinimum(collatedValues)) {
					_action.run(computer.computedValue(), collatedValues, message, levelToSeverity(), this._alertType, _sendEmail);
					_fired = true;
					last.didFireARule = true;
					
					if (last.firedEventTypes == null)
						last.firedEventTypes = new HashSet<String>();
					
					last.firedEventTypes.add(this._eventType);
					
					return true;
				}
			} else if (!_sticky) {
				
				// clear if no longer applicable and not sticky
				
				_fired = false;
			}
			
		} 
		
		return false;
	}
	
	private List<T> allValuesCollectionInterval(Stack<Snapshot<T>> collatedValues) {
	
		List<T> aggregate = new ArrayList<T>();
		
		for (T o : collatedValues.lastElement().values.values()) {
			
			aggregate.add(o);
		}
		
		return aggregate;
	}
	
	private boolean doOccurrencesExceedMinimum(Stack<Snapshot<T>> history) {
		
		int count = 0;
		
		for (Snapshot<T> s : history) {
			if (s.violationLevel != -1) {
				count += 1;
			}
		}
		
		return count >= _minOccurrences;
	}
	
	private boolean allow(Computer<T> computer) {
	
		return !_fired && computer.eventType().equals(this._eventType) && (System.getProperty("watt.wx.service.alerts.active") == null || !System.getProperty("watt.wx.service.alerts.active").equalsIgnoreCase("false"));
	}
	
	private String levelToSeverity() {
		
		if (_level == 0) {
			return "Info";
		} else if (_level == 1) {
			return "Warning";
		} else if (_level == 2) {
			return "Error";
		} else {
			return "Critical";
		}
	}
	
	public String alertType() {
		return _alertType;
	}
	
	public abstract String description();
	
	public abstract String applyRule(Computer<T> computer, List<T> otherValues);
}
