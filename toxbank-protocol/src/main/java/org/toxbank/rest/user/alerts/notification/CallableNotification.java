package org.toxbank.rest.user.alerts.notification;

import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.util.Hashtable;

import net.idea.modbcum.i.IQueryRetrieval;
import net.idea.modbcum.i.query.IQueryUpdate;
import net.idea.restnet.aa.opensso.OpenSSOServicesConfig;
import net.idea.restnet.db.update.CallableDBUpdateTask;
import net.toxbank.client.resource.Account;

import org.opentox.aa.opensso.OpenSSOToken;
import org.restlet.data.Form;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.toxbank.rest.user.DBUser;
import org.toxbank.rest.user.resource.UserURIReporter;

public class CallableNotification extends CallableDBUpdateTask<DBUser,Form,String> {
	protected UserURIReporter<IQueryRetrieval<DBUser>> reporter;
	protected DBUser user;
	protected INotificationEngine notification;
	
	public INotificationEngine getNotification() {
		return notification;
	}

	public void setNotification(INotificationEngine notification) {
		this.notification = notification;
	}

	public CallableNotification(Method method,DBUser item,UserURIReporter<IQueryRetrieval<DBUser>> reporter,
						Form input,
						String baseReference,
						Connection connection,String token)  {
		super(method, input,connection,token);
		this.reporter = reporter;
		this.user = item;
		this.baseReference = baseReference;
	}

	@Override
	protected DBUser getTarget(Form input) throws Exception {
		return user;
	}

	@Override
	protected IQueryUpdate<Object, DBUser> createUpdate(DBUser user)
			throws Exception {
		if (Method.POST.equals(method)) try {
			OpenSSOServicesConfig config = OpenSSOServicesConfig.getInstance();
			OpenSSOToken ssoToken = new OpenSSOToken(config.getOpenSSOService());
			ssoToken.setToken(getToken());
			Hashtable<String,String> results = new Hashtable<String, String>();
			ssoToken.getAttributes(new String[] {"mail"}, results);
			String email = results.get("mail");
			if ((email==null) || "".equals(email)) 
						throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,String.format("Invalid email address for [%s]",user.getUserName()));
			Account account = new Account();
			account.setService("mailto");
			account.setAccountName(email);
			account.setResourceURL(new URL(String.format("%s:%s",account.getService(),URLEncoder.encode(email))));
			user.addAccount(account);
			if (notification.sendAlerts(user,user.getAlerts(), getToken())) {
			//		return new UpdateAlertSentTimeStamp(user.getAlerts());
				//TODO
			}	
			return null;
		} catch (ResourceException x) {
			throw x;
		} catch (Exception x) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,x);
		} finally {
			
		}
		
		throw new ResourceException(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
	}

	@Override
	protected String getURI(DBUser user) throws Exception {
		return reporter.getURI(user);
	}

	@Override
	protected Object executeQuery(IQueryUpdate<Object, DBUser> query)
			throws Exception {
		/*TODO
		Object result = super.executeQuery(query);
		if (Method.POST.equals(method)) {
		}
		*/
		return null;
	}

	@Override
	protected boolean isNewResource() {
		return false;
	}
}
