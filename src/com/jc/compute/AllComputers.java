package com.jc.compute;

import java.util.HashMap;

import com.wm.data.IData;

public class AllComputers {

	public static AllComputers instance = new AllComputers();
	
	private HashMap<Long, ComputersForTimeInterval> _computers = new HashMap<Long, ComputersForTimeInterval>();
	
	public void add(long timeInterval, int maxSlots, boolean countZeros, String namespace, Computer<Double> computer) {
		
		ComputersForTimeInterval c = _computers.get(timeInterval);
		
		if (c == null) {
			c = new ComputersForTimeInterval(timeInterval, maxSlots, countZeros);
			_computers.put(timeInterval, c);
		}
		
		c.addComputer(namespace, computer);
	}
	
	public void clear() {
		
		for (ComputersForTimeInterval c : _computers.values()) {
			c.stop();
		}
		
		_computers.clear();
	}
	
	
	public void record(String eventType, String tid, String service, String source, double i, boolean start) {
		
		for (ComputersForTimeInterval c : _computers.values()) {
						
			if (start) {
				c.start(eventType, tid, service, source, i);
			} else {
				c.add(eventType, tid, service, source, i);
			}
		}
	}
	
	public IData[] summary() {
		return ServiceHistory.toIData(ServiceHistory.summary(_computers.values()), 0);
	}
	
	public IData[] report(String service, String type, long yscale) {
		return ServiceHistory.toIData(ServiceHistory.historyForServiceAndType(_computers.values(), service, type), yscale);
	}
}
