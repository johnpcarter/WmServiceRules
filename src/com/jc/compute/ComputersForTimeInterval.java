package com.jc.compute;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class ComputersForTimeInterval {
	
	public interface ComputerSnapshot {
	
		public String type();
		public String source();
		public String uom();
		public List<Rule<Double>> getRules();
		public Set<String> getRecordedServices();
		public Map<String, Stack<Snapshot<Double>>> getHistory();
		public Stack<Snapshot<Double>> getHistoryForService(String key);
	}
	
    protected ArrayList<ComputersByKey> _computers = new ArrayList<ComputersByKey>();
    private CleanupThread _cleanupThread = null;
   	private int _maxSlots = 10;
	private boolean _countZeros = true;
	
	public ComputersForTimeInterval(long timeInterval, int maxSlots, boolean countZeros) {
	      
		_maxSlots = maxSlots;
		_cleanupThread = new CleanupThread(timeInterval);
	    _countZeros = countZeros;
	    	    
	    _cleanupThread.start();
	}
	
	public void stop() {
		_cleanupThread.requestStop();
	}
	
	public long timeInterval() {
		return _cleanupThread._timeInterval;
	}
	
	public void addComputer(String namespace, Computer<Double> c) {
		
		boolean didMatch = false;
		
		for (ComputersByKey ec : _computers) {
			if (ec.source().equals(c.source()) && ec.namespace().equals(namespace) && ec._prototype.getClass().equals(c.getClass())) {
				didMatch = true;
				ec.addRules(c.rules());
			}
		}
		
		if (!didMatch)
			_computers.add(new ComputersByKey(namespace, c));
	}
	
	public void start(String eventType, String id, String key, String source, Double value) {
	
		for (ComputersByKey c : _computers) {
						
			if (c.source().equals(source) && key.startsWith(c.namespace())) {				
				c.add(eventType, id, key, value, true);
			}
		}
	}
	
	public void add(String eventType, String id, String key, String source, Double value) {
		
		for (ComputersByKey c : _computers) {
						
			if (c.source().equals(source) && key.startsWith(c.namespace())) {				
				c.add(eventType, id, key, value, false);
			}
		}
	}

	public void add(String eventType, String id, String key, Map<String, Double> values) {
		
		for (ComputersByKey c : _computers) {
			for (String k : values.keySet()) {
				if (c.source().equals(k) && key.startsWith(c.namespace())) {
					c.add(eventType, id, key, values.get(k), false);
				}
			}
		}
	}
	
	public List<ComputerSnapshot> getComputers() {
		
		List<ComputerSnapshot> out = new ArrayList<ComputerSnapshot>();
		
		for (ComputersByKey c : _computers) {
			out.add(c);
		}
		
		return out;
	}
	
	public List<Stack<Snapshot<Double>>> getHistory(String key) {
		
		ArrayList<Stack<Snapshot<Double>>> h = new ArrayList<Stack<Snapshot<Double>>>();
		
		for (ComputersByKey c : _computers) {
			h.add(c.getHistoryForService(key));
		}
		
		return h;
	}
	
	protected class ComputersByKey implements ComputerSnapshot {
		
		private Computer<Double> _prototype;
		
		private HashMap<String, ComputersGroupedByEvent> _values = new HashMap<String, ComputersGroupedByEvent>();
		private HashMap<String, Stack<Snapshot<Double>>> _history = new HashMap<String, Stack<Snapshot<Double>>>();
				
		protected ComputersByKey(String namespace, Computer<Double> c) {
			_prototype = c;
			_prototype.setNamespace(namespace);
		}
		
		public void addRules(List<Rule<Double>> rules) {
			
			_prototype.addRules(rules);
		}

		public String namespace() {
			return _prototype.namespace();
		}
		
		public String type() {
			return _prototype.getClass().getSimpleName();
		}
		
		public String source() {
			return _prototype.source();
		}
		
		public String uom() {
			return _prototype.uom();
		}

		private void add(String eventType, String id, String key, Double value, boolean isStartEvent) {

			ComputersGroupedByEvent ec = _values.get(key);
			
			if (ec == null) {
				ec = new ComputersGroupedByEvent(key);
				_values.put(key, ec);
			} 
			
			ec.record(eventType, id, key, value, _prototype, isStartEvent);
		}
		
		public List<Rule<Double>> getRules() {
			
			if (_prototype != null) {
				return _prototype.rules();
			} else {
				return null;
			}
		}
		
		public Map<String, Stack<Snapshot<Double>>> getHistory() {
			
			Map<String, Stack<Snapshot<Double>>> h = new HashMap<String, Stack<Snapshot<Double>>>();
			
			for (String k : _history.keySet()) {
				h.put(k, getHistoryForService(k));
			}
			
			return h;
		}
		
		public Set<String> getRecordedServices() {
			return _history.keySet();
		}
		
		public Stack<Snapshot<Double>> getHistoryForService(String key) {
			
			return _history.get(key);
		}
		
		 private void collateValue(long timePeriod) {
			 
			 synchronized (_values) {
		               
				 // determine which intervals are ready for collection
				 
		    	long timeNow = new Date().getTime();
		    	ArrayList<ComputersGroupedByEvent> computersToCollate = new ArrayList<ComputersGroupedByEvent>();
		    		
		    	for (String key : _values.keySet()) {
		    		long time = _values.get(key).timestamp;
		    			
		    		if (timeNow - time > timePeriod) {
		    			computersToCollate.add(_values.get(key));
		    		}
		    	}
		    		
		    	// archive interval set current to 0 ready for next interval
		    	
		    	for (ComputersGroupedByEvent computersGroupedByEvent : computersToCollate) {
		    			
		    		_values.remove(computersGroupedByEvent.key);
		    		
		    		if (_countZeros) {
		    			_values.put(computersGroupedByEvent.key, computersGroupedByEvent.copyForTimePeriod(timeNow));
		    		}
		    		
		    		Stack<Snapshot<Double>> historyForKey = _history.get(computersGroupedByEvent.key);
		    			
		    		if (historyForKey == null) {
		    			historyForKey = new Stack<Snapshot<Double>>();
		    			_history.put(computersGroupedByEvent.key, historyForKey);
		    		} else if (historyForKey.size() > _maxSlots) {
		    			historyForKey.remove(0);
		    		}
		    				
		    		
		    		historyForKey.add(new Snapshot<Double>(computersGroupedByEvent.values()));
		    	}
		    	
		    	// rinse and spin, i.e. now apply rules
		    	
		    	for (ComputersGroupedByEvent e : computersToCollate) {
		    		
		    		e.applyRules(_history.get(e.key));
		    	}
			 }
		 }
	}

	private class ComputersGroupedByEvent {
		
		public long timestamp;
		public String key;
		public HashMap<String, Computer<Double>> computers = new HashMap<String, Computer<Double>>();
		
		public ComputersGroupedByEvent(String key) {
			this.key = key;
			this.timestamp = new Date().getTime();
		}
		
		public ComputersGroupedByEvent copyForTimePeriod(long time) {
			
			ComputersGroupedByEvent c = new ComputersGroupedByEvent(this.key);
			c.timestamp = time;
			
			for (String e : computers.keySet()) {
				c.computers.put(e, computers.get(e).copy());
			}
			
			return c;
		}
		
		public void record(String eventType, String id, String key, double value, Computer<Double> prototype, boolean isStartEvent) {
			
			Computer<Double> c = null;
			
			synchronized(this) {
				if ((c=computers.get(eventType)) == null) {
					c = prototype.copy(key, eventType);
					computers.put(eventType, c);
				}
			}
			
			c.add(id, value, isStartEvent);
		}
		
		public Map<String, Double> values() {
			
			HashMap<String, Double> out = new HashMap<String, Double>();
			
			for (String eventType : computers.keySet()) {
				out.put(eventType, computers.get(eventType).computedValue());
			}
			
			return out;
		}
		
		public Set<String> firedEvents() {

			HashSet<String> out = new HashSet<String>();
			
			for (Computer<Double> c : computers.values()) {
				
				if (c.didFireRule()) 
					out.add(c.eventType());
			}
			
			return out;
			
		}
		
		public void applyRules(Stack<Snapshot<Double>> values) {
			
			for (Computer<Double> computer : computers.values()) {
				computer.applyRules(values);
			} 
		}
	}
	
	private class CleanupThread extends Thread {
	       		        
		private boolean _requestStop = false;
	    private long _timeInterval = 5000;
	    
	    public CleanupThread(long timeInterval) {
	       
	    	super("com.jc.recordpersistance.HashMapWithTimeLimit$CleanupThread");
	    	_timeInterval = timeInterval;
	    }
	        
	    @Override
	    public void run() {
	    
	    	while (!_requestStop) {
	        
	    		for (ComputersByKey c : _computers) {
	    			c.collateValue(_timeInterval);
	    		}
	    		
	                
	            try {
	            	sleep(_timeInterval);
	            } catch (InterruptedException e) {
	                _requestStop = true;
	            }
	       }
	    }
	        
	    public void requestStop() {
	    	_requestStop = true;
	    }
	 
	} // End inner class : CleanupThread
}
