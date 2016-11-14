package cl.beauty.requestable;


import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cl.beauty.requestable.annotations.RequestableOperation;
import cl.beauty.requestable.enums.EnumRequestableType;
import cl.beauty.requestable.exceptions.RequestableException;
import cl.beauty.requestable.exceptions.RequestableSecurityException;
import cl.beauty.util.serialize.JSONException;



/**
 * Servlet implementation class RequestableService
 */
@WebServlet("/beauty.requestable.service")
public class RequestableService extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static final List<String> onProgress = new ArrayList<String>();
	public static final Object sync = new Object();

    /**
     * Default constructor. 
     */
    public RequestableService() {
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(final ServletConfig config) throws ServletException {
		//load all requetable class
	    (new Thread(new Runnable() {
			
			@Override
			public void run() {
				System.setProperty("ServletUtil.showDebug","off");
				ServletUtil.findAllRequestableClass(config.getServletContext());
			}
		})).start();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		ServletOutputStream out = response.getOutputStream();
		RequestableResponse resp = new RequestableResponse();
		String idCurrentOperation = null;
		String identifier = null;
		String operationName = null;
		boolean isTransaccional;
		try{
		//ServletUtil.printParameters(request);
		idCurrentOperation = getIdCurrentOperation(request);
		
		//usuario		= Login.getUsuario(request);
		//connection	= Database.getBaseConnection(request);
		//auditor		= new Auditor(request);
		//messager	= new Messager(xmlDoc);
	   // if(connection == null)
	    //	throw new RequestableException(String.format("ERROR: No se pudo crear java.sql.Connection para Servicio='%s'.",identifier));
	
		//ServletUtil.findAllRequestableClass(request.getServletContext());
		
		//Obtiene identificadores del Servicio
		identifier 		= request.getParameter("identifier");
		operationName 	= request.getParameter("operation");
		//System.out.println(String.format("[AjaxService] Usuario Conectado %s:%s, Usando Servicio:%s.%s ", usuario.getId_user(),usuario.getUsername(),identifier,identifier,	operationName));

		if(identifier == null || identifier.equals(""))
			throw new RequestableException("ERROR: identifier no tiene un valor válido.");
		if(operationName == null || operationName.equals(""))
			throw new RequestableException("ERROR: operation no tiene un valor válido.");
		
		//Obtiene la clase del servicio
		Class<?> serviceClass = ServletUtil.getRequestableClass(identifier,EnumRequestableType.SERVICE);
		if(serviceClass == null)
			throw new RequestableException(String.format("ERROR: El Servicio='%s' NO existe.",identifier));

		//ValidadorPermisos valida = new ValidadorPermisos(serviceClass.getAnnotation(RequestableClass.class), usuario, connection);
		//permisosUsuario	= valida.createPermisos(usuario.getPermisos());
		
		//Obtiene la operación
		Method operation = ServletUtil.getRequestableMethod(serviceClass, operationName);
		if(operation == null)
			throw new RequestableException(String.format("ERROR: El Servicio='%s' NO define Operación=%s.",identifier,operationName));
		//valida.checkAllowAcces(operation); 
		addOnProgress(idCurrentOperation,operation);
		
		RequestableOperation annotation = operation.getAnnotation(RequestableOperation.class);
		
		//verifica si la operacion es trannsaccional
		/*isTransaccional = annotation.isTransactional();
		if(isTransaccional){
			connection.setAutoCommit(false);
			connection = new RequestableTransactionalConnection(serviceClass.getName(), operationName, connection);
		}*/
		
		//obtiene parametros de la operación
		String[] paramNames		= annotation.parameters();
		Object[] paramValues	= ServletUtil.getRequestableMethodParams(request, response, operation);
		
		//Obtiene interceptores de la operación
		//RequestableOperationInterceptor[] interceptors = operation.getAnnotationsByType(RequestableOperationInterceptor.class);
		
		//invoca PRE interceptors
		//invokeInterceptors(serviceClass,operationName,interceptors,InterceptorPre.class,request,response,xmlDoc,paramNames,paramValues,null);
		
		//Crea instancia del servicio
		Object service = serviceClass.newInstance();
		//injectDependencies(request, response, xmlDoc, service);			
		
		//invoca operación del servicio
		Object serviceResponse = ServletUtil.invokeRequestableMethod(service, operation, paramValues);			

		//invoca POS interceptors
		//invokeInterceptors(serviceClass,operationName,interceptors,InterceptorPos.class,request,response,xmlDoc,paramNames,paramValues,null);
		
		//invoca RESPONSE interceptors
		//serviceResponse = invokeInterceptors(serviceClass,operationName,interceptors,InterceptorResponse.class,request,response,xmlDoc,paramNames,paramValues,serviceResponse);
		
		//audita
		//auditor.saveAuditorias();
		
		//envia respuesta
		//XslUtil.createJsonResult(xmlDoc,serviceResponse);
		
		/*if(isTransaccional){
			((RequestableTransactionalConnection)connection).getConnection().commit();
		}
		endOK = true;*/
		resp.setResult("OK");
		resp.setData(serviceResponse);
		}catch(RequestableException  | RequestableSecurityException e){
			resp.addMessage(e.getMessage());
		}catch (IllegalArgumentException e) {
			System.err.println("[AjaxService] EXCEPCION CONTROLADA: ");
			e.printStackTrace();
			resp.addMessage(String.format("ERROR: Los parámetros entregados para la operación:%s no son válidos.",e.getMessage()));
		}catch (JSONException e) {
			Throwable cause = e;
			while(cause.getCause() != null)
				cause = cause.getCause();
			resp.addMessage(String.format("ERROR: No se pudo transformar uno de los parámetros al tipo esperado.\nDetalle: %s.",cause.toString()));
		}catch(Throwable e){
			System.err.println("[AjaxService] EXCEPCION CONTROLADA: ");
			e.printStackTrace();
			resp.addMessage(String.format("Error executing %s.%s.\nDetalle:%s",identifier,operationName,e.toString()));
		}finally{
			removeOnProgress(idCurrentOperation);
			
			/*if(isTransaccional){
				connection = ((RequestableTransactionalConnection)connection).getConnection();
				if(!endOK){
					try {connection.rollback();}
					catch (SQLException e) {
						System.out.println("Error connection.rollback()");
					}
				}
			}
			try {connection.setAutoCommit(true);}
			catch (SQLException e) {
				System.out.println("Error connection.setAutoCommit(true)");
			}*/
		}
		
		response.setContentType("application/json"); 
		out.print(resp.parseJson());
	}
		private void removeOnProgress(String idCurrentOperation) {
			synchronized (sync) {
				try{
					onProgress.remove(onProgress.indexOf(idCurrentOperation));
				}catch(IndexOutOfBoundsException e){}
			}
		}

		private void addOnProgress(String idCurrentOperation,Method operation) throws RequestableException {
			synchronized (sync) {
				if(onProgress.contains(idCurrentOperation)){
					RequestableOperation annotation = operation.getAnnotation(RequestableOperation.class);
					if(!annotation.allowMultipleRequest())
						throw new RequestableException(String.format("La operación:%s no permite múltiples request.",annotation.identifier()),RequestableException.REQUEST_ALREADY_ON_PROCESS);

				}else
					onProgress.add(idCurrentOperation);
			}
		}
	private String getIdCurrentOperation(HttpServletRequest request) {
		
		StringBuilder params = new StringBuilder("{\"JSESSIONID\":\"");
		params.append(request.getSession().getId()).append("\"");
		Enumeration<String> par = request.getParameterNames();
		while(par.hasMoreElements()){
			String param = par.nextElement();
			if(request.getParameter(param).startsWith("{") || request.getParameter(param).startsWith("["))
				params.append(String.format(",\"%s\":%s", param,request.getParameter(param)));
			else
				params.append(String.format(",\"%s\":\"%s\"", param,request.getParameter(param)));
		}
		params.append("}");
		
		return params.toString();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
