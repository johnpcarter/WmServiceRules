package com.jc.compute.computers;

import com.jc.compute.Computer;
import com.jc.compute.Rule;
import com.jc.compute.ComputersForNamespace.EventType;

public class Average extends AbstractComputer<Double> {

	private Long _total = null;
	private Long _errorTotal = null;
	
	private int _count = 0;
	
	public Average(Source source) {
		super(source);
	}

	@Override
	public Computer<Double> copy() {
		
		Average t = new Average(_source);

		if (_total != null)
			t._total = 0l;
		
		if (_errorTotal != null)
			t._errorTotal = 0l;
		
		for (Rule<Double> r : this._rules) {
			t._rules.add(r.clone());
		}
				
		return t;
	}

	@Override
	public int type() {
		return 1;
	}
	
	@Override
	public boolean includesEventType(EventType eventType) {
		
		return (eventType == EventType.AuditEvent && _total != null) ||
				(eventType == EventType.ExceptionEvent && _errorTotal != null);
	}
	
	@Override
	public void addEventType(EventType eventType) {
	
		if (eventType == EventType.AuditEvent) {
			_total = 0l;
		} else {
			_errorTotal = 0l;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public EventTypeComputer<Double>[] eventTypes() {
		
		EventTypeComputer<Double>[] out = null;
		
		if (_total == null || _errorTotal == null) {
			out = new EventTypeComputer[1];
		} else {
			out = new EventTypeComputer[2];
		}
		
		int i = 0;
		
		if (_total != null)
			out[i++] = auditEventTypeComputer();
		
		if (_errorTotal != null)
			out[i] = exceptionEventTypeComputer();
		
		return out;
	}

	@Override
	public void record(EventType eventType, Number value) {
		
		if (eventType == EventType.AuditEvent) {
			this._total += value.longValue();
		} else {
			this._errorTotal += value.longValue();
		}
		
		this._count += 1;
	}
	
	protected Double computedValueForAuditEvent() {
		
		if (_count > 0) {
			return (double) (_total / _count);
		} else {
			return 0d;
		}
	}
	
	protected Double computedValueForExceptionEvent() {
		
		if (_count > 0) {
			return (double) (_errorTotal / _count);
		} else {
			return 0d;
		}
	}
}
