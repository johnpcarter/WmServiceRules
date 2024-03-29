package com.jc.compute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.jc.compute.Computer.Source;
import com.jc.compute.ComputersForNamespace.EventType;
import com.wm.data.IData;
import com.wm.data.IDataCursor;
import com.wm.data.IDataFactory;
import com.wm.data.IDataUtil;

public class AllComputers {
	
	public static AllComputers instance = new AllComputers();
	
	private Date _startTime = new Date();
	
	private HashMap<Long, ComputersForNamespace> _computersByTimeInterval = new HashMap<Long, ComputersForNamespace>();
	
	private HashMap<String, String> _pipelineAtributesForAuditEvents = new HashMap<String, String>();
	private HashMap<String, String> _pipelineAtributesForExceptionEvents = new HashMap<String, String>();

	public void add(long timeInterval, EventType eventType, boolean topLevelServicesOnly, int maxSlots, boolean countZeros, String namespace, String pipelineAttribute, Computer<Number> computer, String[] excludeList, String[] includeList, long transactionDuration, String persistService) {
		
		ComputersForNamespace cti = _computersByTimeInterval.get(timeInterval);
		
		if (cti == null) {
			cti = new ComputersForNamespace(timeInterval, topLevelServicesOnly, maxSlots, countZeros, excludeList, includeList, transactionDuration, persistService);
			_computersByTimeInterval.put(timeInterval, cti);
		}
		
		if (pipelineAttribute != null && pipelineAttribute.length() > 0) {
			String key = namespace == null ? "*" : namespace;
			
			if (eventType == EventType.AuditEvent) {
				this._pipelineAtributesForAuditEvents.put(key, pipelineAttribute);
			} else {
				this._pipelineAtributesForExceptionEvents.put(key, pipelineAttribute);
			}
		}
		
		cti.addComputer(eventType, namespace, pipelineAttribute, computer);
	}
	
	public String pipelineAttributeForNamespace(EventType eventType, String service) {
		
		String key = null;
		HashMap<String, String> pipelineAtributes = this._pipelineAtributesForAuditEvents;
		
		if (eventType != EventType.AuditEvent) {
			pipelineAtributes = this._pipelineAtributesForExceptionEvents;
		}
		
		for (String namespace : pipelineAtributes.keySet()) {
						
			if (namespace.length() == 0 || service.startsWith(namespace)) {
				key = pipelineAtributes.get(namespace);
				break;
			}
		}
		
		return key;
	}
	
	public void clear() {
		
		for (ComputersForNamespace c : _computersByTimeInterval.values()) {
			c.stop();
		}
		
		_computersByTimeInterval.clear();
		
		_pipelineAtributesForAuditEvents.clear();
		_pipelineAtributesForExceptionEvents.clear();
	}
	
	public Date startTime() {
		return _startTime;
	}
	
	public Collection<ComputersForNamespace> computers() {
		return this._computersByTimeInterval.values();
	}
	
	public boolean record(EventType eventType, String service, Long duration, boolean isTopLevelService) {
		
		boolean didRecordAtLeastOne = false;
				
		for (ComputersForNamespace cti : _computersByTimeInterval.values()) {
					
			if (cti.record(eventType, service, duration, isTopLevelService))
				didRecordAtLeastOne = true;
		}
		
		return didRecordAtLeastOne;
	}
	
	public IData[] firedRules() {
		
		ArrayList<IData> out = new ArrayList<IData>();

		for (ComputersForNamespace cti : _computersByTimeInterval.values()) {
			
			Collection<Map<String, Collection<Rule<?>>>> rs = cti.firedRules();
			
			for (Map<String, Collection<Rule<?>>> m : rs) {
				
				for (String service : m.keySet()) {
					
					ArrayList<IData> rules = new ArrayList<IData>();

					for (Rule<?> r : m.get(service)) {
						rules.add(r.toIData());
					}
					
					IData o = IDataFactory.create();
					IDataCursor c = o.getCursor();
					IDataUtil.put(c, "service", service);
					IDataUtil.put(c,  "rules", rules.toArray(new IData[rules.size()]));
					c.destroy();
				}
			}
		}
		
		return out.toArray(new IData[out.size()]);
	}

	public void clearStickyRule(String serviceName, String computerName, Source source, String ruleDescription) {
		
		for (ComputersForNamespace cti : _computersByTimeInterval.values()) {
			cti.clearStickyRule(serviceName, computerName, source, ruleDescription);
		}
	}

	public IData totalsFor(String filter) {
		return ServiceHistory.toIDataTable(ServiceHistory.summary(null, filter, AllComputers.instance.computers()), _startTime);
	}
	
	public IData[] servicesFor(Source source, String filter) {
		return ServiceHistory.toIData(ServiceHistory.summary(source, filter, _computersByTimeInterval.values()), 0);
	}
	
	public IData[] serviceStatisticsFor(String service, Source source, long yscale) {
		return ServiceHistory.toIData(ServiceHistory.historyForServiceAndType(_computersByTimeInterval.values(), service, source), yscale);
	}
}
