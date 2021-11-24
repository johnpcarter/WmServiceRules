package com.jc.compute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import com.jc.compute.ComputersForTimeInterval.ComputersByKey;
import com.jc.compute.GroupSnapshot.Value;
import com.wm.data.IData;
import com.wm.data.IDataCursor;
import com.wm.data.IDataFactory;
import com.wm.data.IDataUtil;

public class ServiceHistory {

	public  final String[] COLORS = {"blue", "green", "brown", "pink"};

	public String name;
	
	private HashMap<String, Type> _types = new HashMap<String, Type>();
	
	ServiceHistory(String name) {
		this.name = name;
	}
	
	public static Collection<ServiceHistory> summary(Collection<ComputersForTimeInterval> computers) {
		
		return make(computers, null, null, true);
	}

	public static Collection<ServiceHistory> historyForServiceAndType(Collection<ComputersForTimeInterval> computers, String service, String type) {
		
		return make(computers, service, type, false);
	}
	
	static Collection<ServiceHistory> make(Collection<ComputersForTimeInterval> computers, String service, String type, boolean summaryOnly) {
	
		HashMap<String, ServiceHistory> out = new HashMap<String, ServiceHistory>();
		
		long maxInterval = 0;
		for (ComputersForTimeInterval cm : computers) {
			if (cm.timeInterval() > maxInterval)
				maxInterval = cm.timeInterval();
		}
		
		for (ComputersForTimeInterval cm : computers) {
			
			for (ComputersByKey c : cm._computers) {
				
				Set<String> services = c.getRecordedServices();
				
				for (String svc : services) {
					
					if (service == null || service.equals(svc)) {
						ServiceHistory h = out.get(svc);
					
						if (h == null) {
							h = new ServiceHistory(svc);
							out.put(svc, h);
						}
					
						if (type == null || type.equals(c.type())) {
							h.add(c.type(), c.uom(), c.source(), c.getHistoryForService(svc), maxInterval, summaryOnly);
						}
					}
				}
			}
		}
			
		return out.values();
	}
	
	public static IData[] toIData(Collection<ServiceHistory> collection, double scale) {
		
		IData[] out = new IData[collection.size()];
		int i = 0;
		for (ServiceHistory h : collection) {
			out[i++] = h.toIData(scale);
		}
		
		return out;
	}
	
	public void add(String type, String uom, String source, Stack<Snapshot<Double>> values, long maxInterval, boolean summaryOnly) {
		
		Type t = _types.get(type);
		
		if (t == null) {
			t = new Type(type, uom, source);
			_types.put(type, t);
		}
		
		if (!summaryOnly) {
			t.add(values, maxInterval);
		}
	}

	public IData toIData(double scale) {
	
		IData out = IDataFactory.create();
		IDataCursor c = out.getCursor();
		
		IDataUtil.put(c, "name", name);
		IData[] types = new IData[_types.size()];
		int i = 0;
		for (Type t: _types.values()) {
			types[i++] = t.toIData(scale);
		}
		
		IDataUtil.put(c, "types", types);
		c.destroy();
		
		return out;
	}
	
	public class Type {
		

		public String name;
		public String uom;
		public String source;
		
		public Set<String> eventTypes = new HashSet<String>();
		
		private ArrayList<GroupSnapshot> _intervals = new ArrayList<GroupSnapshot>();

		public Type(String name, String uom, String source) {
			this.name = name;
			this.uom = uom;
			this.source = source;
		}
		
		public IData toIData(double scale) {
			
			// figure out colors for label in intervals
			
			HashMap<String, String> labelColors = new HashMap<String, String>();
			
			int z = 0;
			for (GroupSnapshot g : _intervals) {
				for (Value v : g.values) {
					if (labelColors.get(v.label) == null) {
						
						if (v.label.startsWith("Exception")) {
							labelColors.put(v.label, "orange");
						} else {
							labelColors.put(v.label, COLORS[z++]);
						
							if (z > 4)
								z = 0;
						}
					}
				}
			}
			
			IData out = IDataFactory.create();
			IDataCursor c = out.getCursor();
			
			IDataUtil.put(c, "name", name);
			IDataUtil.put(c, "uom", uom);
			IDataUtil.put(c, "source", source);
			
			IData[] wrapper = new IData[eventTypes.size()];
			int i = 0;
			for (String label : labelColors.keySet()) {
				IData e = IDataFactory.create();
				IDataCursor ec = e.getCursor();
				IDataUtil.put(ec, "label", label);
				IDataUtil.put(ec, "color", labelColors.get(label));
				ec.destroy();
				
				if (z > 4) 
					z = 0;
				
				wrapper[i++] = e;
			}
			
			IDataUtil.put(c, "eventTypes", wrapper);
			
			scale = adjustScaleForBiggestValue(scale);
			
			IData[] snaps = new IData[_intervals.size()];
			i = 0;
			for (GroupSnapshot interval : _intervals) {
				snaps[i++] = interval.toIData(labelColors, scale);
			}
			
			if (snaps.length > 0) {
				IDataUtil.put(c, "intervals", snaps);
			}
			
			c.destroy();
			
			return out;
		}
		
		protected void add(Stack<Snapshot<Double>> values, long maxInterval) {
			
			for (Snapshot<Double> s : values) {
				
				GroupSnapshot g = snapshotForTime(s.time, maxInterval);
				
				for (String eventType : s.values.keySet()) {
					
					if (s.firedEventTypes != null && s.firedEventTypes.contains(eventType)) {
						g.add(eventType, s.values.get(eventType), s.violationLevel);
					} else {
						g.add(eventType, s.values.get(eventType));
					}
				
					this.eventTypes.add(eventType);
				}
			}
		}
		
		private GroupSnapshot snapshotForTime(Date time, long maxInterval) {
			
			GroupSnapshot g = null;
			
			for (int i = 0; i < _intervals.size(); i++) {
				
				if (Math.abs(_intervals.get(i).time.getTime() - time.getTime()) < maxInterval) {
					g = _intervals.get(i);
				} else if (_intervals.get(i).time.after(time)) {
					// inject
					
					g = new GroupSnapshot(time);
					
					_intervals.add(i, g);
				}
				
				if (g != null) {
					break;
				}
			}
			
			// reached end if g still null, add it to the end
			
			if (g == null) {
				g = new GroupSnapshot(time);
				_intervals.add(g);
			}
			
			return g;
		}
		
		private double adjustScaleForBiggestValue(double scale) {
			
			double max = 0;
				
			for (GroupSnapshot g : _intervals) {
					
				for (Value v : g.values) {
					if (v.value > max) {
						max = v.value;
					}
				}
			}
			
			if (scale > 0 && max > 0) {
				max = max / scale;
			}

			return max;
		}
	}
}
