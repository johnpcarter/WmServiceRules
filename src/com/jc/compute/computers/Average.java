package com.jc.compute.computers;

import com.jc.compute.Computer;
import com.jc.compute.Rule;

public class Average extends AbstractComputer<Double> {

	private double _total = 0;
	private int _count = 0;
	
	public Average(String source, String uom, double value) {
		super(source, uom);
		
		if (value > 0) {
			_total = value;
			_count = 1;
		}
	}

	@Override
	public Computer<Double> copy() {
		
		Average t = new Average(_source, _uom, 0);
		t._namespace = this._namespace;
		t._running = _running;

		for (Rule<Double> r : this._rules) {
			t._rules.add(r.clone());
		}
				
		return t;
	}
	
	@Override
	public Computer<Double> copy(String key, String eventType) {
		
		Average a = (Average) this.copy();
		
		a._key = key;
		a._eventType = eventType;
		
		return a;
	}

	@Override
	public void add(Double value) {
		
		if (value > 0) {
			_total += value;
			_count += 1;
		}
	}
	
	@Override
	Double combined(Double first, Double last) {
		
		return last - first;
	}
	
	@Override
	public Double computedValue() {
		
		if (_count > 0) {
			return _total / _count;
		} else {
			return 0d;
		}
	}
}
