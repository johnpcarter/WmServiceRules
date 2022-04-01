package com.jc.compute.computers;

import com.jc.compute.Computer;
import com.jc.compute.ComputersForNamespace.EventType;
import com.jc.compute.Rule;

public class Total extends AbstractComputer<Long> {

	public Total(Source source) {
		super(source);
	}

	private Long _total = null;
	private Long _errorTotal = null;
		
	@Override
	public Computer<Long> copy() {
		
		Total t = new Total(_source);
		
		if (_total != null)
			t._total = 0l;
		
		if (_errorTotal != null)
			t._errorTotal = 0l;
		
		for (Rule<Long> r : this._rules) {
			t._rules.add(r.clone());
		}
				
		return t;
	}

	@Override
	public int type() {
		return 0;
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
	public EventTypeComputer<Long>[] eventTypes() {
		
		EventTypeComputer<Long>[] out = null;
		
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
	}

	@Override
	protected Long computedValueForAuditEvent() {
		return this._total;
	}

	@Override
	protected Long computedValueForExceptionEvent() {
		return this._errorTotal;
	}
}
