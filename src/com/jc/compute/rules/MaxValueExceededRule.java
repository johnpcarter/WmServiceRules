package com.jc.compute.rules;

import java.util.List;

import com.jc.compute.Computer;
import com.jc.compute.ComputersForNamespace.EventType;
import com.jc.compute.Rule;
import com.wm.app.b2b.server.ServiceException;

public class MaxValueExceededRule extends Rule<Number> {

	private Double _maxValue;
	
	public MaxValueExceededRule(EventType eventType, String alertType, String service, int minOccurrences, Double maxValue, boolean isSticky, int level, boolean sendEmail) throws ServiceException {
		
		super(eventType, new InvokeServiceRuleAction<Number>(service), minOccurrences, isSticky, level, sendEmail);
		_alertType = alertType;
		_maxValue = maxValue;
	}

	@Override
	public Rule<Number> clone() {
		
		try {
			return new MaxValueExceededRule(this._eventType, this._alertType, _action.namespace(), this.minOccurences(), this._maxValue, this._sticky, this._level, this._sendEmail);
		} catch (ServiceException e) {
			throw new RuntimeException(e);
		} 
	}
	
	@Override
	public String applyRule(Computer<Number> computer, List<Number> otherValues) {
				
		Number lastValue = computer.computedValue(_eventType);
		
		if (lastValue.longValue() > _maxValue) {
			 return "last value " + lastValue + " exceeded threshhold " + _maxValue + " for " + computer.key();

		} else {
			return null;
		}
	}

	@Override
	public String description() {
		
		if (minOccurences() == 1) {
			return "triggered if instances exceed " + _maxValue;
		} else {
			return "triggered if instances exceed " + _maxValue + " in " + minOccurences() + " intervals";
		}
	}
}
