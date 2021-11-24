package com.jc.compute.rules;

import java.util.Stack;

import com.jc.compute.RuleAction;
import com.jc.compute.Snapshot;
import com.wm.app.b2b.server.ISRuntimeException;
import com.wm.app.b2b.server.InvokeState;
import com.wm.app.b2b.server.ServerAPI;
import com.wm.app.b2b.server.Service;
import com.wm.app.b2b.server.ServiceException;
import com.wm.app.b2b.server.Session;
import com.wm.app.b2b.server.UnknownServiceException;
import com.wm.app.b2b.server.User;
import com.wm.app.b2b.server.UserManager;
import com.wm.data.IData;
import com.wm.data.IDataCursor;
import com.wm.data.IDataFactory;
import com.wm.data.IDataUtil;

public class InvokeServiceRuleAction<T> implements RuleAction<T> {

	private String _ns;
	private String _ifc;

	public InvokeServiceRuleAction(String service) throws ServiceException {
		
		if (service == null || !service.contains(":"))
    		throw new ServiceException("Invalid service : " + service);
    	
    	_ns = service.substring(0, service.indexOf(":"));
     	_ifc = service.substring(service.indexOf(":")+1);
	}
	
	@Override
	public String namespace() {
		return _ns + ":" + _ifc;
	}
	
	@Override
	public void run(T lastValue, Stack<Snapshot<T>> history, String message, String severity, String alertType,  boolean sendEmail) {

		IData pipeline = IDataFactory.create();
		IDataCursor c = pipeline.getCursor();
		IDataUtil.put(c, "alertType", alertType);
		IDataUtil.put(c, "severity", severity);
		IDataUtil.put(c, "message", message);
		IDataUtil.put(c, "sendMail", sendEmail);

		IDataUtil.put(c,  "lastValue", "" + lastValue);
		
		IData[] h = new IData[history.size()];
		int i = 0;
		for (Snapshot<T> s : history) {
			IData sn = IDataFactory.create();
			IDataCursor sc = sn.getCursor();
			IDataUtil.put(sc, "time", s.time);
			
			IData[] values = new IData[s.values.size()];
			for (String e : s.values.keySet()) {
				IData vi = IDataFactory.create();
				IDataCursor vic = vi.getCursor();
				IDataUtil.put(vic, "event", e);
				IDataUtil.put(c, "value", s.values.get(e));
				IDataUtil.put(c, "didFire", s.firedEventTypes != null && s.firedEventTypes.contains(e));
				vic.destroy();
			}
			
			IDataUtil.put(sc, "values", values);
			IDataUtil.put(sc, "didFire", s.didFireARule);
			sc.destroy();
			
			h[i++] = sn;
		}
		
		IDataUtil.put(c, "history", h);
		c.destroy();
		
		try {
			invokeService(pipeline, _ns, _ifc, false);
		} catch (UnknownServiceException | ServiceException e) {
			ServerAPI.logError(e);
		}
	}

	private static boolean invokeService(IData inputPipeline, String ns, String svc, boolean ignoreErrors) throws UnknownServiceException, ServiceException 
    {
    	 try 
         {
 // Bug in wm, means that if we try to invoke services outside of service-pool we get a null pointer
 // exception. Following if fixes the problem by ensuring we have session and user assigned.

    		 if (InvokeState.getCurrentState() == null || InvokeState.getCurrentState().getSession() == null)
    		 { 
    			 setInvokeStateFor("Administrator");
    		 }
    		 
    		 //if (rootContextId != null) // allows async integrations to be linked via an optional root context id / activation id
    		 //	 setRootContextIdForThread(rootContextId);
    		 
    		 Service.doInvoke(ns, svc, inputPipeline);

    		 return true;
         } 
    	 catch (UnknownServiceException e) // manage invalid name-space
         {
             throw e;
         }  
    	 catch (ServiceException e) 
         {
             if (ignoreErrors)
                 return false;
             else
                 throw e;
         } 
    	 catch(ISRuntimeException e)
    	 {
    		 throw e;
    	 }
    	 catch(RuntimeException e)
    	 {
    		 e.printStackTrace();
    		 throw new ISRuntimeException(e);	// make sure adapter based runtime exceptions are propagated without wrapping
    	 }
         catch (Exception e) 
         {
             throw new ServiceException(e);	// some other type of service occurred
         }
    }  
	
	protected static void setInvokeStateFor(String userId)
    {
    	setInvokeStateFor(UserManager.getUser(userId));	
    }
    
    protected static void setInvokeStateFor(User user)
    {
    	Session session = Service.getSession();
    	
    	if (session == null)
    		session = new Session("esb");
    	
    	InvokeState state = InvokeState.getCurrentState();
    	if (state == null)
    	{
    		state = new InvokeState();
    		InvokeState.setCurrentState(state);
    	}
    	
		state.setSession(session);
		InvokeState.setCurrentUser(user);
    }
    
    @SuppressWarnings("unused")
	private static void setRootContextIdForThread(String activationId)
    {
    	InvokeState is = InvokeState.getCurrentState();
 			 
 		if (is != null)
 		{
     		String[] args = null;

 			if (is.getAuditRuntime() != null)
 			{
 				args = is.getAuditRuntime().getContextStack();
 				
 				if (args.length <= WM_ROOT_CONTEXT_ID_INDEX)
 					args = new String[WM_ROOT_CONTEXT_ID_INDEX+1];
 				
				args[WM_ROOT_CONTEXT_ID_INDEX] = activationId;
         		InvokeState.getCurrentState().getAuditRuntime().setContextStack(args);
 			}
 		}
    }
    
	/** 
	 * Used to identify the webMethods root context id based in runtime-attribute array
	 * returned by InvokeState. Attention this will have to be tested for each webMethods
	 * version as this is not official.
	 */
	public static final int			WM_ROOT_CONTEXT_ID_INDEX = 0;
	

	public static final String 		SERVER_ID_PROPERTY = "server.id";
}
