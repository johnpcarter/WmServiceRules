package com.jc.compute;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.jc.compute.Computer.Source;
import com.jc.compute.ComputersForNamespace.ComputersByService;
import com.jc.compute.ComputersForNamespace.EventType;
import com.wm.data.IData;
import com.wm.data.IDataCursor;
import com.wm.data.IDataFactory;
import com.wm.data.IDataUtil;

public class ServiceHistory {

	public static final String[] COLORS = {"blue", "green", "brown", "pink"};

	public String name;
	
	private HashMap<Integer, Type> _types = new HashMap<Integer, Type>();
	
	private long _totalCount;
	private long _totalErrors;
	private long _totalTransactions;
	
	ServiceHistory(String name) {
		this.name = name;
	}
	
	public static Collection<ServiceHistory> summary(Source source, String filter, Collection<ComputersForNamespace> computers) {
		
		return make(computers, null, filter, source, true);
	}

	public static Collection<ServiceHistory> historyForServiceAndType(Collection<ComputersForNamespace> computers, String service, Source source) {
		
		return make(computers, service, null, source, false);
	}
	
	static Collection<ServiceHistory> make(Collection<ComputersForNamespace> computers, String service, String filter, Source source, boolean summaryOnly) {
	
		HashMap<String, ServiceHistory> out = new HashMap<String, ServiceHistory>();
		
		long maxInterval = 0;
		for (ComputersForNamespace cm : computers) {
			if (cm.timeInterval() > maxInterval)
				maxInterval = cm.timeInterval();
		}
		
		for (ComputersForNamespace cm : computers) {
			ServiceHistory.make(cm._computers.values(), service, filter, source, summaryOnly, maxInterval, out);
		}
			
		return out.values();
	}
	
	public static void make(Collection<ComputersByService> cs, String service, String filter, Source source, boolean summaryOnly, long maxInterval, HashMap<String, ServiceHistory> out) {
	
		for (ComputersByService c : cs) {
			Set<String> services = c.getRecordedServices();
			
			for (String svc : services) {
					
				if ((service == null || svc.equals(service)) &&
						(filter == null || svc.startsWith(filter))) {
					ServiceHistory h = out.get(svc);
					
					if (h == null) {
						h = new ServiceHistory(svc);
						out.put(svc, h);
					}
					
					h.add(source, c.getHistoryForService(svc), maxInterval, summaryOnly);
				}
			}	
		}
	}
	
	public static IData toIDataTable(Collection<ServiceHistory> collection, Date startTime) {
		
		long totalCount = 0;
		long totalErrors = 0;
		long totalTransactions = 0;
		
		IData[] rows = new IData[collection.size()];
		int i = 0;
		for (ServiceHistory h : collection) {
			
			rows[i++] = h.toIDataTable();
				
			totalCount += h._totalCount;
			totalErrors += h._totalErrors;
			totalTransactions += h._totalTransactions;	
		}
		
		IData out = IDataFactory.create();
		IDataCursor c = out.getCursor();
		IDataUtil.put(c, "rows", rows);
		IDataUtil.put(c,  "trackedServices", "" + rows.length);
		IDataUtil.put(c,  "totalCount", "" + totalCount);
		IDataUtil.put(c,  "totalErrors", "" + totalErrors);
		IDataUtil.put(c,  "totalTransactions", "" + totalTransactions);

		IDataUtil.put(c, "from", formatDate(startTime));
		IDataUtil.put(c, "fromDate", startTime);
		IDataUtil.put(c, "to", formatDate(new Date()));
		IDataUtil.put(c, "toDate", new Date());

		c.destroy();
		
		return out;
	}

	public static IData[] toIData(Collection<ServiceHistory> collection, long scale) {
		
		IData[] out = new IData[collection.size()];
		int i = 0;
		for (ServiceHistory h : collection) {
			out[i++] = h.toIData(scale);
		}
		
		return out;
	}
	
	public void add(Source source, Collection<Snapshot<Number>> snapshots, long maxInterval, boolean summaryOnly) {
		
		for (Snapshot<Number> snapshot : snapshots) {
			add(source, snapshot, maxInterval, summaryOnly);
		}
	}

	public void add(Source source, Snapshot<Number> snapshot, long maxInterval, boolean summaryOnly) {
		
		for (Snapshot<Number>.Value value : snapshot.values) {
			
			if (source == null || source == value.source) {
										
				int key = value.type + (value.source.ordinal()*10);
				
				Type t = _types.get(key);

				if (t == null) {
					t = new Type(value.type, value.source);
					_types.put(key, t);
				}
					
				if (!summaryOnly) {
						
					if (snapshot.firedEventTypes != null && snapshot.firedEventTypes.contains(value.eventType)) {
						t.add(value.eventType, source, snapshot.time, value.value, maxInterval, snapshot.violationLevel);
					} else {
						t.add(value.eventType, source,  snapshot.time, value.value, maxInterval);
					}							
				} else {
					t.addTotal(value.eventType, value.value);
				}
			}
		}
	}
	
	public IData toIDataTable() {
		
		IData out = IDataFactory.create();
		IDataCursor c = out.getCursor();
		
		IDataUtil.put(c, "name", name);
		for (Type t: _types.values()) {
			
			if (t.source == Source.Count) {
				if (t.id == 0) {
					IDataUtil.put(c, "totalCount", "" + t.auditCount);
							
					this._totalCount += t.auditCount;
					
					if (t.errorCount > 0)
						IDataUtil.put(c, "totalErrors", "" + t.errorCount);

					this._totalErrors += t.errorCount;

				} else if (t.id == 1) {
					IDataUtil.put(c, "averageCount", "" + t.auditCount);				
				}
			} else if (t.source == Source.Duration) {
				if (t.id == 0) {
					IDataUtil.put(c, "totalDuration", "" + t.auditCount);
				} else if (t.id == 1) {
					IDataUtil.put(c, "averageDuration","" +  t.auditCount);
				}
			} else if (t.source == Source.Transaction) {
				if (t.id == 0) {
					IDataUtil.put(c, "totalTransactions", "" + t.auditCount);
					
					this._totalTransactions += t.auditCount;

				} else if (t.id == 1) {
					IDataUtil.put(c, "averageTransactions","" +  t.auditCount);
				}
			}
		}
		
		c.destroy();
		
		return out;
	}
	
	public IData toIData(long scale) {
	
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
		

		public int id;
		public Source source;
		
		public long auditCount = 0;
		public long errorCount = 0;
		
		public Set<String> eventTypes = new HashSet<String>();
		
		private ArrayList<GroupSnapshot<Number>> _intervals = new ArrayList<GroupSnapshot<Number>>();

		public Type(int id, Source source) {
			this.id = id;
			this.source = source;
		}
		
		public IData toIData(double scale) {
			
			// figure out colors for label in intervals
			
			HashMap<String, String> labelColors = new HashMap<String, String>();
			
			int z = 0;
			for (GroupSnapshot<? extends Number> g : _intervals) {
				for (GroupSnapshot<? extends Number>.Value v : g.values) {
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
			//IDataUtil.put(c, "uom", uom);
			IDataUtil.put(c, "source", source.toString());
			
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
			
			if (wrapper.length > 0) 
				IDataUtil.put(c, "eventTypes", wrapper);
			
			scale = adjustScaleForBiggestValue(scale);
			
			IData[] snaps = new IData[_intervals.size()];
			i = 0;
			for (GroupSnapshot<? extends Number> interval : _intervals) {
				snaps[i++] = interval.toIData(labelColors, scale);
			}
			
			if (snaps.length > 0) {
				IDataUtil.put(c, "intervals", snaps);
			}
			
			IDataUtil.put(c, "auditTotal", auditCount);
			IDataUtil.put(c, "errorTotal", errorCount);

			c.destroy();
			
			return out;
		}
		
		protected void add(EventType eventType, Source source, Date time, Number v, long maxInterval) {
			
			GroupSnapshot<Number> g = snapshotForTime(time, maxInterval);
			
			g.add(eventType.toString(), v);
			this.eventTypes.add(eventType.toString());	
		}
		
		protected void addTotal(EventType eventType, Number v) {
		
			switch (eventType) {
			case AuditEvent:
				this.auditCount += v.longValue();
				break;
			case ExceptionEvent:
				if (this.source == Source.Transaction)
					this.auditCount += v.longValue();
				else
					this.errorCount += v.longValue();
			default:
				break;
			}
		}
		
		protected void add(EventType eventType, Source source, Date time, Number v, long maxInterval, int violationLevel) {
			
			GroupSnapshot<Number> g = snapshotForTime(time, maxInterval);
			
			g.add(eventType.toString(), v, violationLevel);
			this.eventTypes.add(eventType.toString());	
		}
		
		private GroupSnapshot<Number> snapshotForTime(Date time, long maxInterval) {
			
			GroupSnapshot<Number> g = null;
			
			for (int i = 0; i < _intervals.size(); i++) {
				
				if (Math.abs(_intervals.get(i).time.getTime() - time.getTime()) < maxInterval) {
					g = _intervals.get(i);
				} else if (_intervals.get(i).time.after(time)) {
					// inject
					
					g = new GroupSnapshot<Number>(time);
					
					_intervals.add(i, g);
				}
				
				if (g != null) {
					break;
				}
			}
			
			// reached end if g still null, add it to the end
			
			if (g == null) {
				g = new GroupSnapshot<Number>(time);
				_intervals.add(g);
			}
			
			return g;
		}
		
		private double adjustScaleForBiggestValue(double scale) {
			
			double max = 0;
				
			for (GroupSnapshot<? extends Number> g : _intervals) {
					
				for (GroupSnapshot<? extends Number>.Value v : g.values) {
					
					if (v.value != null) {
					if (v.value.doubleValue() > max) {
						max = v.value.doubleValue();
					}
					} else {
						System.out.println("wtf");
					}
				}
			}
			
			if (scale > 0 && max > 0) {
				max = max / scale;
			}

			return max;
		}
	}
	
	private static String formatDate(Date date) {
		DateFormat fmt = new SimpleDateFormat("dd MMM - HH:mm");
		return fmt.format(date);
	}
}
