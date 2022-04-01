package com.jc.compute.computers;


import com.jc.compute.Computer;
import com.jc.compute.ComputersForNamespace.EventType;
import com.jc.compute.Rule;

public class Difference extends AbstractComputer<Long> {

	private Long _first = null;
	private Long _last = null;
	
	private Long _firstError = null;
	private Long _lastError = null;
	
	public Difference(Source source) {
		super(source);
	}

	@Override
	public Computer<Long> copy() {
		
		Difference t = new Difference(_source);

		if (_first != null) {
			_first = 0l;
			_last = 0l;
		}
		
		if (_firstError != null) {
			_firstError = 0l;
			_lastError = 0l;
		}
		
		for (Rule<Long> r : this._rules) {
			t._rules.add(r.clone());
		}
				
		return t;
	}
	
	@Override
	public int type() {
		return 2;
	}
	

	@Override
	public boolean includesEventType(EventType eventType) {
		
		return (eventType == EventType.AuditEvent && _first != null) ||
				(eventType == EventType.ExceptionEvent && _firstError != null);
	}
	
	@Override
	public void addEventType(EventType eventType) {
	
		if (eventType == EventType.AuditEvent) {
			_first = -1l;
		} else {
			_firstError = -1l;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public EventTypeComputer<Long>[] eventTypes() {
		
		EventTypeComputer<Long>[] out = null;
		
		if (_first == null || _firstError == null) {
			out = new EventTypeComputer[1];
		} else {
			out = new EventTypeComputer[2];
		}
		
		int i = 0;
		
		if (_first != null)
			out[i++] = auditEventTypeComputer();
		
		if (_firstError != null)
			out[i] = exceptionEventTypeComputer();
		
		return out;
	}
	
	@Override
	public void record(EventType eventType, Number value) {
		
		if (_first == null) {
			if (_first == -1)
				_first = value.longValue();
			else 
				_last = value.longValue();
		} else {
			if (_firstError == -1)
				_firstError = value.longValue();
			else 
				_lastError = value.longValue();
		}
	}
	
	@Override
	public Long computedValue(EventType eventType) {
		
		if (_last != null) {
			return _last - _first;
		} else if (_first != null) {
			return (long) _first;
		} else {
			return 0l;
		}
	}

	@Override
	protected Long computedValueForAuditEvent() {
		
		if (_last != null)
			return _last - _first;
		else
			return _first;
	}

	@Override
	protected Long computedValueForExceptionEvent() {
		
		if (_lastError != null) 
			return _lastError - _firstError;
		else
			return _firstError;
	}
}
