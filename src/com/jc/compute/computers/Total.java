package com.jc.compute.computers;

import com.jc.compute.Computer;
import com.jc.compute.Rule;

public class Total extends AbstractComputer<Double> {

	private double _total = 0;
	
	public Total(String source, String uom, double value) {
		super(source, uom);
		_total = value;
	}
	
	@Override
	public Computer<Double> copy() {
		
		Total t = new Total(_source, _uom, 0);
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
	public String namespace() {
		return _namespace;
	}
	
	@Override
	public void add(Double value) {
		
		_total += value;		
	}

	@Override
	Double combined(Double first, Double last) {
		
		return last - first;
	}
	
	@Override
	public Double computedValue() {
		
		return _total;
	}
}
