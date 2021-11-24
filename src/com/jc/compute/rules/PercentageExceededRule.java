package com.jc.compute.rules;

import java.util.List;

import com.jc.compute.Computer;
import com.jc.compute.Rule;
import com.wm.app.b2b.server.ServiceException;

public class PercentageExceededRule extends Rule<Double> {

	private Double _maxValue;
	
	public PercentageExceededRule(String eventType, String alertType, String service, int minOccurrences, Double maxValue, boolean isSticky, int level, boolean sendEmail) throws ServiceException {
		super(eventType, new InvokeServiceRuleAction<Double>(service), minOccurrences, isSticky, level, sendEmail);
		
		_alertType = alertType;
		_maxValue = maxValue;
	}

	@Override
	public Rule<Double> clone() {
		
		try {
			return new PercentageExceededRule(this._eventType, this._alertType,  _action.namespace(), this.minOccurences(), this._maxValue, this._sticky, this._level, this._sendEmail);
		} catch (ServiceException e) {
			throw new RuntimeException(e);
		} 
	}
	
	@Override
	public String applyRule(Computer<Double> computer, List<Double> otherValues) {
				
		Double lastValue = computer.computedValue();
		
		Double tot = 0d;
		
		for (Double o : otherValues) {
			tot += o;
		}
				
		Double percent = 0d;
		
		if (tot > 0)
			percent = (lastValue / tot) * 100;
		
		if (percent >= _maxValue) {
			 return "last value is " + percent + "% of total, exceeded threshhold " + _maxValue + "% for " + computer.key();
		} else {
			return null;
		}
	}

	@Override
	public String description() {
		
		if (minOccurences() == 1) {
			return "triggered if instances exceed percentage of total by " + _maxValue + "%";
		} else {
			return "triggered if instances exceed percentage of total by " + _maxValue + "% over " + minOccurences() + " intervals";
		}
	}
}
