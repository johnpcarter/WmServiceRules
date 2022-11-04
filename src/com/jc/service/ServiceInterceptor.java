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
import com.wm.data.IDataCursor;
import com.wm.data.IDataUtil;
import com.wm.util.ServerException;

public class ServiceInterceptor implements InvokeChainProcessor {
	
	public static final String PRT_START_SERVICE = "wm.prt.dispatch:handlePublishedInput";
	public static final String PRT_STEP_SERVICE = "wm.prt.dispatch:handleTransition";
	public static final String PROCESS_MODEL_ID = "ProcessModelID";
	public static final String PROCESS_STEP_ID = "TargetStepID";
	public static final String DOCUMENT_TYPE = "documentType";

	
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
			
			if (serviceName.equals(PRT_START_SERVICE) || serviceName.equals(PRT_STEP_SERVICE)) {
			
				// process start and steps need to be tracked using the process info, not the generic service names
				
				System.out.println("************* PRT service detected");

				IDataCursor c = pipeline.getCursor();
				if (c.first() && c.getValue() instanceof IData) {
					IData payload = (IData) c.getValue();
					IDataCursor pc = payload.getCursor();
					
					System.out.println("************* payload doc is " + c.getKey());
					
					if (c.getKey().equals("JMSMessage")) {
												
						if (serviceName.equals(PRT_START_SERVICE)) {
						
							IData properties = IDataUtil.getIData(pc,  "properties");
							IDataCursor propertiesCursor = properties.getCursor();
							String docType = IDataUtil.getString(propertiesCursor, DOCUMENT_TYPE);
							
							if (docType != null) {
								serviceName = "bpm_init:" + docType;// + ":" + step;
							} else {
								propertiesCursor.first();
								do {
									System.out.println("************* found " + propertiesCursor.getKey() + " = " + propertiesCursor.getValue());
								} while (!propertiesCursor.next());
							}
							
							propertiesCursor.destroy();

							System.out.println("************* wm.prt.dispatch:handlePublishedInput - " + serviceName);

						} else {
							
							IData header = IDataUtil.getIData(pc,  "header");
							IDataCursor headerCursor = header.getCursor();

							String model = IDataUtil.getString(headerCursor, "JMSDestination");
							
							if (model != null) {
								serviceName = "bpm_step:" + model;// + ":" + step;
							} else {
								headerCursor.first();
								do {
									System.out.println("************* found " + headerCursor.getKey() + " = " + headerCursor.getValue());
								} while (!headerCursor.next());
							}
							
							System.out.println("************* wm.prt.dispatch:handleTransition - " + model + " / " + model);						}						
					} else {
					
						if (serviceName.equals(PRT_START_SERVICE)) {
							// tricky because it could be any kind of document
							
							// so let's use the name of the input i.e. the document Type
							
							if (pc.first()) {
								serviceName = "bpm_init:" + pc.getKey();
							}
							
							System.out.println("************* wm.prt.dispatch:handlePublishedInput - " + pc.getKey());

						} else {
							String model = IDataUtil.getString(pc, PROCESS_MODEL_ID);
							String step = IDataUtil.getString(pc, PROCESS_STEP_ID);
							
							if (model != null) {
								serviceName += "bpm_step:" + model;// + ":" + step;
							} else {
								pc.first();
								do {
									System.out.println("************* found " + pc.getKey() + " = " + pc.getValue());
								} while (!pc.next());
							}
							
							System.out.println("************* wm.prt.dispatch:handleTransition - " + model + " / " + step);
						}
					}
					
					pc.destroy();
				} else {
					System.out.println("************* oops payload is not an IData " + c.getKey());
				}
				
				c.destroy();
				
			} else {
				String key = AllComputers.instance.pipelineAttributeForNamespace(EventType.AuditEvent, serviceName);
			
				if (key != null) {
					
					// will use pipeline value for given key to further index collection (if found)
				
					IDataCursor c = pipeline.getCursor();
					Object v = IDataUtil.get(c, key);
					c.destroy();
					
					//System.out.println("*************** Looked up key '" + key + "' got '" + v + "'");
				
					if (v != null) {
						serviceName += ":" + v;
					}
				}
			}
			
			if(chain.hasNext()) {
				
				((InvokeChainProcessor) chain.next()).process(chain, svc, pipeline, status);
			
	            AllComputers.instance.record(EventType.AuditEvent, serviceName, System.currentTimeMillis() - status.getStartTime(), isTopLevelService);
			 }
		} catch (Exception e) {
			
			String key = AllComputers.instance.pipelineAttributeForNamespace(EventType.ExceptionEvent, serviceName);

			if (key != null) {
				
				// will use pipeline value for given key to further index collection (if found)
				
				IDataCursor c = pipeline.getCursor();
				Object v = IDataUtil.get(c, key);
				c.destroy();
					
				if (v != null && !serviceName.endsWith(v.toString())) {
					serviceName += ":" + v;
				}
			}
			
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
