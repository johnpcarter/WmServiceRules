package com.jc.service;

import java.util.Iterator;

import com.jc.compute.AllComputers;
import com.jc.compute.ComputersForNamespace.EventType;
import com.wm.app.b2b.server.BaseService;
import com.wm.app.b2b.server.InvokeState;
import com.wm.app.b2b.server.invoke.InvokeChainProcessor;
import com.wm.app.b2b.server.invoke.InvokeManager;
import com.wm.app.b2b.server.invoke.ServiceStatus;
import com.wm.data.IData;
import com.wm.util.ServerException;

public class ServiceInterceptor implements InvokeChainProcessor {
	
	
	public static ServiceInterceptor    defaultInstance;
	    	    
	public static void register() {
	    	
		if (defaultInstance == null) {
			defaultInstance = new ServiceInterceptor();
		}
		
	    InvokeManager.getDefault().registerProcessor(defaultInstance);
	}
	    
	public static void unregister() {
		InvokeManager.getDefault().unregisterProcessor(defaultInstance);
	}
	
	public ServiceInterceptor() {
	}
	    
	@Override
	public void process(@SuppressWarnings("rawtypes") Iterator chain, BaseService svc, IData pipeline, ServiceStatus status) throws ServerException {
		
		String serviceName = getServiceName(svc);
		boolean isTopLevelService = status.isTopService();

		try {
			 if(chain.hasNext()) {
				 ((InvokeChainProcessor) chain.next()).process(chain, svc, pipeline, status);
			
	             AllComputers.instance.record(EventType.AuditEvent, serviceName, System.currentTimeMillis() - status.getStartTime(), isTopLevelService);
			 }
		} catch (Exception e) {		
			AllComputers.instance.record(EventType.ExceptionEvent, serviceName, System.currentTimeMillis() - status.getStartTime(), isTopLevelService);
			throw e;
		}
	}
	
	protected String getServiceName(BaseService baseService) {
		return baseService.getNSName().getFullName();
	}
	
	protected String[] getContextIDsForService() 
	    {
	        String[] contextIDs = {null, null, null};

	        try {
	            InvokeState currentInvokeState = InvokeState.getCurrentState();
	            //Stack<?> servicesStack = currentInvokeState.getCallStack();
	            String contextIDStack[] = currentInvokeState.getAuditRuntime().getContextStack();

	            String contextId = null;
	            String parentContextId = null;
	            String rootContextId = null;

	            int contextId_index = contextIDStack.length - 1;


	            contextId = contextIDStack[contextId_index];
	            if (contextId_index > 0) {
	                parentContextId = contextIDStack[contextId_index - 1];
	            }
	            rootContextId = contextIDStack[0];

	            contextIDs[0] = contextId;
	            contextIDs[1] = parentContextId;
	            contextIDs[2] = rootContextId;
	        } catch (Exception e) {
	            throw new RuntimeException(e);
	        }

	        return contextIDs;
	    }
}
