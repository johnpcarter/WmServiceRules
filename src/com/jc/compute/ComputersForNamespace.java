package com.jc.compute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.jc.compute.Computer.EventTypeComputer;
import com.jc.compute.Computer.Source;
import com.jc.compute.rules.InvokeServiceRuleAction;
import com.wm.app.b2b.server.ServerAPI;
import com.wm.app.b2b.server.ServiceException;
import com.wm.app.b2b.server.UnknownServiceException;
import com.wm.data.IData;

public class ComputersForNamespace {
	
	public enum EventType {
		AuditEvent,
		ExceptionEvent
	}
	
	public interface ComputerSnapshot {
	
		public Set<String> getRecordedServices();
		public Map<String, Stack<Snapshot<Number>>> getHistory();
		public Stack<Snapshot<Number>> getHistoryForService(String key);
	}
	
	public static int TransactionDuration = 3000;

    protected HashMap<String, ComputersByService> _computers = new HashMap<String, ComputersByService>();
    private CleanupThread _cleanupThread = null;
   	private int _maxSlots = 10;
   	private boolean _topLevelOnly = false;
	private boolean _countZeros = true;
	protected String[] _excludeList;
	protected HashSet<String> _includeList;
	protected long _transactionDuration = TransactionDuration = 3000;
	
	private String _persistService = null;
	
	public ComputersForNamespace(long timeInterval, boolean topLevelServicesOnly, int maxSlots, boolean countZeros, String[] excludeList, String[] includeList, long transactionDuration, String persistService) {
	      
		_maxSlots = maxSlots;
		_topLevelOnly = topLevelServicesOnly;
		_cleanupThread = new CleanupThread(timeInterval);
	    _countZeros = countZeros;
	    _excludeList = excludeList;
	    _persistService = persistService;
	    
		if (includeList != null) {
			this._includeList = new HashSet<String>(includeList.length);
			for (String i : includeList) {
				this._includeList.add(i);
			}
		}
		
		if (transactionDuration > 0) 
			_transactionDuration = transactionDuration;
		
	    _cleanupThread.start();
	}
	
	public void stop() {
		_cleanupThread.requestStop();
	}
	
	public long timeInterval() {
		return _cleanupThread._timeInterval;
	}
	
	public void addComputer(EventType eventType, String namespace, String pipelineAtribute, Computer<Number> c) {
		
		ComputersByService ec = _computers.get(namespace);
		
		if (ec == null) {
			ec = new ComputersByService();
			_computers.put(namespace, ec);
		}
		
		ec.addComputer(eventType, c);
	}
	
	public boolean record(EventType eventType, String service, Long duration, boolean isTopLevelService) {
	
		if ((isTopLevelService || !_topLevelOnly) && !this.isExcluded(service)) {
			
			boolean didRecordAtLeastOne = false;

			for (String namespace : this._computers.keySet()) {
				
				if ((namespace.equals("*") && !this.isServiceCompatibleWithOtherComputers("*", service)) || service.startsWith(namespace)) {
					ComputersByService cs = this._computers.get(namespace);
					for (Source source : Source.values()) {
						if (cs.record(eventType, source, service, duration))
							didRecordAtLeastOne = true;
					}
				}
				
			}
			
			return didRecordAtLeastOne;
		} else {
			return false;
		}
		
	}
	
	public Collection<Map<String, Collection<Rule<?>>>> firedRules() {
		
		ArrayList<Map<String, Collection<Rule<?>>>> rules = new ArrayList< Map<String, Collection<Rule<?>>>>();
		
		for (ComputersByService c : this._computers.values()) {
			rules.add(c.firedRules());
		}
		
		return rules;
	}

	public void clearStickyRule(String serviceName, String computerName, Source source, String ruleDescription) {
		
		for (ComputersByService c : this._computers.values()) {
			c.clearStickyRule(serviceName, computerName, source, ruleDescription);
		}
	}
	
	public List<ComputerSnapshot> getComputers() {
		
		List<ComputerSnapshot> out = new ArrayList<ComputerSnapshot>();
		
		for (ComputersByService c : _computers.values()) {
			out.add(c);
		}
		
		return out;
	}
	
	public List<Stack<Snapshot<Number>>> getHistory(String key) {
		
		ArrayList<Stack<Snapshot<Number>>> h = new ArrayList<Stack<Snapshot<Number>>>();
		
		for (ComputersByService c : _computers.values()) {
			h.add(c.getHistoryForService(key));
		}
		
		return h;
	}
	
	private boolean isServiceCompatibleWithOtherComputers(String filter, String service) {
		
		boolean didMatch = false;
				
				
		for (String f : this._computers.keySet()) {
		
			if (!f.equals(filter) && service.startsWith(f)) {
				didMatch = true;
			}
		}
		
		return didMatch;
	}
	
	private boolean isExcluded(String service) {
		
		if (this._excludeList != null) {
			
			if (this._includeList != null && this._includeList.contains(service)) {
				return false;
			} else {
				boolean exclude = false;
				for (String x : this._excludeList) {
					if (service.startsWith(x)) {
						exclude = true;
						break;
					}
				}
				
				return exclude;
			}
		}
		
		return false;
	}
	
	protected class ComputersByService implements ComputerSnapshot {
		
		protected HashMap<String, HashMap<Integer, Computer<Number>>> _prototypes = new HashMap<String, HashMap<Integer, Computer<Number>>>();

		private HashMap<String, ComputersGroupedSource> _values = new HashMap<String, ComputersGroupedSource>();
		private HashMap<String, Stack<Snapshot<Number>>> _history = new HashMap<String, Stack<Snapshot<Number>>>();
		
		public void addComputer(EventType eventType, Computer<Number> computer) {
			
			HashMap<Integer, Computer<Number>> ca = _prototypes.get(computer.source().toString());
			
			if (ca == null) {
				ca = new HashMap<Integer, Computer<Number>>();				
				this._prototypes.put(computer.source().toString(), ca);
			} 
			
			Computer<Number> c = ca.get(computer.type());
			
			if (c == null) {
				ca.put(computer.type(), computer);
				c = computer;
			} else {
				c.addRules(computer.rules());
			}
			
			if (!c.includesEventType(eventType)) {
				c.addEventType(eventType);
			}
		}

		public Collection<Computer<Number>> getComputers(Source source) {
			
			String key = source.toString();
			HashMap<Integer, Computer<Number>> p = null;
			
			if ((p=_prototypes.get(key)) != null)
				return p.values();
			else
				return null;
		}
		
		public boolean record(EventType eventType, Source source, String serviceName, Number value) {
			
			ComputersGroupedSource ec = _values.get(serviceName);
			
			if (ec == null) {
				ec = new ComputersGroupedSource(serviceName);
				_values.put(serviceName, ec);
			}
						
			return ec.record(eventType, source, value);
		}
		
		public Map<String, Stack<Snapshot<Number>>> getHistory() {
			
			Map<String, Stack<Snapshot<Number>>> h = new HashMap<String, Stack<Snapshot<Number>>>();
			
			for (String k : _history.keySet()) {
				h.put(k, getHistoryForService(k));
			}
			
			return h;
		}
		
		public Map<String, Collection<Rule<?>>> firedRules() {
			
			HashMap<String, Collection<Rule<?>>> out = new HashMap<String, Collection<Rule<?>>>();
			
			for (String serviceName : _values.keySet()) {
				ComputersGroupedSource ec = _values.get(serviceName);
				out.put(serviceName, ec.firedRules());
			}
			
			return out;
		}
		
		public void clearStickyRule(String serviceName, String computerName, Source source, String ruleDescription) {
		
			ComputersGroupedSource ec = _values.get(serviceName);
			
			if (ec != null) {
				ec.clearStickyRule(computerName, source, ruleDescription);
			}
		}
		
		public Set<String> getRecordedServices() {
			return _history.keySet();
		}
		
		public Stack<Snapshot<Number>> getHistoryForService(String serviceName) {
			
			return _history.get(serviceName);
		}
		
		 private Map<String, Snapshot<Number>> collateValues(long timePeriod) {
			
			 if (_values == null)
				 return null;
			 
			 HashMap<String, Snapshot<Number>> values = new HashMap<String, Snapshot<Number>>();
			 
			 synchronized (_values) {
		               
				 // determine which intervals are ready for collection
				 
		    	long timeNow = new Date().getTime();
		    	ArrayList<ComputersGroupedSource> computersToCollate = new ArrayList<ComputersGroupedSource>();
		    		
		    	for (String key : _values.keySet()) {
		    		long time = _values.get(key).timestamp;
		    			
		    		if (timeNow - time > timePeriod) {
		    			computersToCollate.add(_values.get(key));
		    		}
		    	}
		    		
		    	// archive interval set current to 0 ready for next interval
		    	
		    	for (ComputersGroupedSource computersGroupedByEvent : computersToCollate) {
		    			
		    		_values.remove(computersGroupedByEvent.serviceName);
		    		
		    		if (_countZeros) {
		    			_values.put(computersGroupedByEvent.serviceName, computersGroupedByEvent.copyForTimePeriod(timeNow));
		    		}
		    		
		    		Stack<Snapshot<Number>> historyForKey = _history.get(computersGroupedByEvent.serviceName);
		    			
		    		if (historyForKey == null) {
		    			historyForKey = new Stack<Snapshot<Number>>();
		    			_history.put(computersGroupedByEvent.serviceName, historyForKey);
		    		} else if (historyForKey.size() > _maxSlots) {
		    			historyForKey.remove(0);
		    		}
		    		
		    		Snapshot<Number> snapshot = computersGroupedByEvent.snapshot();
		    		
		    		historyForKey.add(snapshot);
		    		values.put(computersGroupedByEvent.serviceName, snapshot);
		    	}
		    	
		    	// rinse and spin, i.e. now apply rules
		    	
		    	for (ComputersGroupedSource e : computersToCollate) {
		    		
		    		e.applyRules(_history.get(e.serviceName));
		    	}
			 }
			 
			 return values;
		 }
		 
		 private class ComputersGroupedSource {
				
				public long timestamp;
				public String serviceName;
				
				public HashMap<String, Computer<Number>> computers = new HashMap<String, Computer<Number>>();

				public ComputersGroupedSource(String serviceName) {
					this.timestamp = new Date().getTime();
					this.serviceName = serviceName;
				}
				
				public ComputersGroupedSource copyForTimePeriod(long time) {
					
					ComputersGroupedSource c = new ComputersGroupedSource(this.serviceName);
					c.timestamp = time;
					
					return c;
				}
				
				public boolean record(EventType eventType, Source source, Number value) {
					
					synchronized(this) {
						
						Collection<Computer<Number>> prototypes = ComputersByService.this.getComputers(source);

						if (prototypes != null) {
							
							for (Computer<Number> prototype : prototypes) {
							
								if (prototype.includesEventType(eventType)) {
								
									Computer<Number> computer = computers.get(prototype.key());
									
									if (computer == null) {
										computer = prototype.copy();
										computers.put(prototype.key(), computer);
									}
									
									computer.record(eventType, calcValue(prototype.source(), value));
								}
							}
							
							return true;
						}
					}
					
					return false;
				}
				
				public Snapshot<Number> snapshot() {
					
					Snapshot<Number> snapshot = new Snapshot<Number>();
										
					if (this.computers != null && this.computers.size() > 0) {						
						for (Computer<Number> c : this.computers.values()) {
							
							for (EventTypeComputer<Number> ec : c.eventTypes()) {
								snapshot.add(ec.eventType(), c.source(), c.type(), ec.computedValue());
							}
						}
					}

					return snapshot;
				}
				
				public Collection<Rule<?>> firedRules() {
					
					ArrayList<Rule<?>> out = new ArrayList<Rule<?>>();
					
					for (Computer<?> c : this.computers.values()) {
						
						for (Rule<?> r : c.rules()) {
							if (r.didFire()) {
								out.add(r);
							}
						}
					}
					
					return out;
				}
				
				public void clearStickyRule(String computerName, Source source, String description) {
				
					Computer<Number> computer = computers.get(computerName + source.toString());

					if (computer != null) {
						computer.clearStickyRule(description);
					}
					
				}
				
				public void applyRules(Stack<Snapshot<Number>> values) {
					
					for (Computer<Number> c : this.computers.values()) {
						for (EventTypeComputer<Number> ec : c.eventTypes()) {
							ec.applyRules(values);
						}
					}
				}
				
				private Number calcValue(Source source, Number value) {
					
					switch (source) {
					case Count:
						return 1;
					case Duration:
						return value;
					case Transaction:
						if (value.longValue() < _transactionDuration) {
							return 1;
						} else {
							double d = value.longValue() / ((double) _transactionDuration);
							return Math.ceil(d);
						}
					default:
						return value;
					}
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
	        	    		
	    		ArrayList<ServiceHistory> records = new ArrayList<ServiceHistory>();
	    		
	    		for (ComputersByService c : _computers.values()) {
	    			
		    		System.out.println("collating ");
		    		
		    		Map<String, Snapshot<Number>> snapshots = c.collateValues(_timeInterval);
		    	
		    		if (snapshots.size() > 0) {
		    			for (String service : snapshots.keySet()) {
		    			
		    				ServiceHistory h = new ServiceHistory(service);
			    			h.add(null, snapshots.get(service), -1, true);

			    			records.add(h);
		    			}
		    		}
	    		}
	    		
	    		if (_persistService != null && records.size() > 0) {
	    			persistTotals(records);
	    		}
	    		
	            try {
	            	sleep(_timeInterval);
	            } catch (InterruptedException e) {
		    		System.out.println("flumped");

	                _requestStop = true;
	            }
	       }
	    }
	        
	    public void requestStop() {
	    	_requestStop = true;
	    }
	    
	    private void persistTotals(ArrayList<ServiceHistory> records) {
	    	
	    	HashMap<String, ServiceHistory> out = new HashMap<String, ServiceHistory>();
    		ServiceHistory.make(_computers.values(), null, null, null, true, -1, out);
    		IData data = ServiceHistory.toIDataTable(records, new Date());
    		
    		String ns = _persistService.substring(0, _persistService.indexOf(":"));
         	String ifc = _persistService.substring(_persistService.indexOf(":")+1);

         	System.out.println("Invoking persistence service " + ns + ":" + ifc);
         	
    		try {
    			InvokeServiceRuleAction.invokeService(data, ns, ifc, false);
    		} catch (UnknownServiceException | ServiceException e) {
    			ServerAPI.logError(e);
    		}
	    }
	 
	} // End inner class : CleanupThread
}
