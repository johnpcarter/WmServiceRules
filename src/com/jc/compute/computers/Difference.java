package com.jc.compute.computers;


import com.jc.compute.Computer;
import com.jc.compute.Rule;

public class Difference extends AbstractComputer<Double> {

	private Double _first = null;
	private Double _last = null;
	
	public Difference(String source, String uom) {
		super(source, uom);
	}
	
	public Difference(String source, String uom, double value) {
		super(source, uom);
		
		_first = value;
	}

	@Override
	public Computer<Double> copy() {
		
		Difference t = new Difference(_source, _uom, 0);
		t._namespace = this._namespace;
		t._running = _running;

		for (Rule<Double> r : this._rules) {
			t._rules.add(r.clone());
		}
				
		return t;
	}
	
	@Override
	public Computer<Double> copy(String key, String eventType) {
		
		Total t = (Total) this.copy();
		
		t._key = key;
		t._eventType = eventType;
		
		return t;
	}

	@Override
	public void add(Double value) {
		
		if (_first == null)
			_first = value;
		else
			_last = value;
	}
	
	@Override
	Double combined(Double first, Double last) {
		
		return last - first;
	}
	
	@Override
	public Double computedValue() {
		
		if (_last != null) {
			return _last - _first;
		} else if (_first != null) {
			return (double) _first;
		} else {
			return 0d;
		}
	}
}
