package cl.beauty.requestable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.annotation.IncompleteAnnotationException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import cl.beauty.util.reflection.UtilReflection;
import cl.beauty.util.serialize.JSONException;
import cl.beauty.util.serialize.JSONUtil;
import cl.beauty.requestable.annotations.RequestableClass;
import cl.beauty.requestable.annotations.RequestableOperation;
import cl.beauty.requestable.enums.EnumRequestableType;
import cl.beauty.requestable.exceptions.RequestableException;
import cl.beauty.requestable.interfaces.ParameterParser;
import cl.beauty.util.stream.StreamUtil;
import cl.beauty.util.string.StringBuilderUtil;
import cl.beauty.util.xml.UtilDOM;

public class ServletUtil {

	private static final Object sync = new Object();
	private static final String IDENTIFIER_FORMAT = "[%s] %s";

	private static Hashtable<String, Class<?>> requestableClass = null;
	
	public static Class<?> getRequestableClass(String identifier,EnumRequestableType type){
		if(requestableClass == null)
			throw new RuntimeException("[ServletUtil] requestableClass has not been initialized properly. You must call ServletUtil.findRequestableClass(context) first.");
		
		return requestableClass.get(String.format(IDENTIFIER_FORMAT, type,identifier));
		
	}
	public static Method getRequestableMethod(Class<?> clss,String methodIdentifier) {
		List<Method> methods = UtilReflection.getAllAnnotatedMethods(clss, RequestableOperation.class);
		RequestableOperation annotation;
		for(Method m:methods){
			annotation = m.getAnnotation(RequestableOperation.class);
			if(annotation.identifier().equals(methodIdentifier))
				return m;
		}
		return null;
	}

	public static Object invokeRequestableMethod(HttpServletRequest request,HttpServletResponse response, Object obj,Method method) throws Throwable  {
		Object[] params = getRequestableMethodParams(request, response, method);
		return invokeRequestableMethod(obj, method, params);
	}
	public static Object invokeRequestableMethod(Object obj, Method method,Object[] params) throws Throwable {
		String methodName = "";
		try{
			methodName = String.format(" %s.%s",method.getDeclaringClass().getSimpleName(), method.getName());
			UtilReflection.methodParamsValidate(method, params);
			return method.invoke(obj, params);	
		}catch(IllegalArgumentException e){
			throw new IllegalArgumentException(methodName,e);
		}catch (InvocationTargetException e) {
			Throwable cause = e;
			while(cause.getCause() != null){
				if(cause.getClass().isAssignableFrom(RequestableException.class))
					break;
				cause = cause.getCause();
			}
			throw cause;
				
		}
		
	}public static Object[] getRequestableMethodParams(HttpServletRequest request,HttpServletResponse response, Method method) throws Throwable {
		RequestableOperation methodAnnotation = method.getAnnotation(RequestableOperation.class);
		Type[]   paramsTypes	= method.getGenericParameterTypes();
		Object[] params			= new Object[paramsTypes.length];
		StringBuilder invokeDetail = new StringBuilder(String.format("[InvokeRequestableMethod] %s.%s",method.getDeclaringClass().getName(), method.getName()));
		try{
			invokeDetail.append("(");
			if( paramsTypes.length > 0 ){
				String   parameterName = null;
				Class<?> parameterType = null; 
				for(int i=0;i< paramsTypes.length;i++){
					try{
						parameterName = methodAnnotation.parameters()[i];
						if(paramsTypes[i] instanceof Class){
							parameterType = (Class<?>)paramsTypes[i];
		
							if(HttpServletRequest.class.isAssignableFrom(parameterType))
								params[i] = request;
							else if(HttpServletResponse.class.isAssignableFrom(parameterType))
								params[i] = response;
							else if(request.getParameter(parameterName)!= null){
								if(ParameterParser.class.isAssignableFrom(parameterType))
									params[i] = ((ParameterParser<?>)parameterType.newInstance()).parse(request.getParameter(parameterName));
								else if(List.class.isAssignableFrom(parameterType)){
									params[i] = JSONUtil.decodeList(request.getParameter(parameterName), parameterType);
								}else 
									params[i] = JSONUtil.decodeObject(request.getParameter(parameterName), parameterType);
							}
						}else if(paramsTypes[i] instanceof ParameterizedType){
							 ParameterizedType parameterizedType = (ParameterizedType) paramsTypes[i];
							 parameterType = (Class<?>)parameterizedType.getActualTypeArguments()[0];
							 params[i] = JSONUtil.decodeList(request.getParameter(parameterName),  parameterType);
						}
						invokeDetail.append(String.format("%s:%s = %s, ",parameterName,parameterType.getCanonicalName(),params[i]));
					}catch(JSONException e){
						throw new JSONException(String.format("Error decodificando parámetro %s: Detalle: %s",parameterName,e.getMessage()));
					}
				}
				StringBuilderUtil.replaceLast(invokeDetail, ", ", "");
			}		
			invokeDetail.append(")");
			//System.out.println(invokeDetail.toString());	
			return params;	
		}catch (ArrayIndexOutOfBoundsException e) {
			throw new RequestableException(String.format("El número de parámetros definidos en el atributo 'parameters' de la operación:%s, NO coinciden con el número del método anotado",methodAnnotation.identifier()), e);
		}
		
	}
	public static void findAllRequestableClass(ServletContext context){
		synchronized (sync) {
			if(requestableClass == null){
				if("on".equals(System.getProperty("ServletUtil.showDebug")))
					System.out.println("[ServletUtil] Finding All Requestable Class");
				requestableClass = new Hashtable<String, Class<?>>();
				String[] classPath = null;
				try{
					classPath = readLocations(context);
				}catch(RuntimeException e){
					System.err.println(String.format("[ServletUtil] %s\n",e.getMessage()));
					System.out.println("[ServletUtil] Se usarán rutas por defecto.");
			    	classPath = new String[2];
					classPath[0] = context.getRealPath("WEB-INF/lib");
					classPath[1] = context.getRealPath("WEB-INF/classes");
				}

				Hashtable<String, List<String>> duplicateClassIdentifier	= new Hashtable<String, List<String>>();
				Hashtable<String, List<String>> duplicateMethodIdentifiers 	= new Hashtable<String, List<String>>();
				List<Class<?>>	requestableClassFound	= UtilReflection.listAnnotadedClass(RequestableClass.class, classPath);
				String classIdentifier;
				String methodIdentifier;
				RequestableClass annotation;
				for(Class<?> c:requestableClassFound){
					annotation = c.getAnnotation(RequestableClass.class);
					try{
						classIdentifier = String.format(IDENTIFIER_FORMAT, annotation.type(),annotation.identifier());
					}catch(IncompleteAnnotationException e){
						int index = e.toString().indexOf("missing");
						throw new RuntimeException(String.format("%s in class annotated %s with RequestableClass",e.toString().substring(index),c.getName()));
					}
					requestableClass.put(classIdentifier, c);
					
					registerDuplicate(duplicateClassIdentifier, classIdentifier, c.getName());
					
					for(Method m:UtilReflection.getAllAnnotatedMethods(c, RequestableOperation.class)){
						methodIdentifier = m.getAnnotation(RequestableOperation.class).identifier();
						registerDuplicate(duplicateMethodIdentifiers, classIdentifier+"."+methodIdentifier, c.getName() +":"+m.getName());
					}
				}
				
				//check for duplicate
				boolean hasDuplicate = checkDuplicate(duplicateClassIdentifier);
				if(hasDuplicate){
					requestableClass = null;
					throw new RuntimeException(String.format("Class Identifier: %s is duplicated",enumToString(duplicateClassIdentifier.keys())));
				}
				hasDuplicate = checkDuplicate(duplicateMethodIdentifiers);
				if(hasDuplicate){
					requestableClass = null;
					throw new RuntimeException(String.format("Class Methods Identifier: %s is duplicated",enumToString(duplicateMethodIdentifiers.keys())));
				}
				if("on".equals(System.getProperty("ServletUtil.showDebug"))){
					Enumeration<String> classes = requestableClass.keys();
					while(classes.hasMoreElements()){
						System.out.println(String.format("[ServletUtil] Requestable Class Found: %s",classes.nextElement()));
					}
				}
			}
		}		
	}
	private static String[] readLocations(ServletContext context) {
		String[] urls = null;
		String url = context.getRealPath("WEB-INF/requestable.xml");
		try {
			Document xml = UtilDOM.createDocument(url);
			List<Node> locations = UtilDOM.searchNode(xml, "locations");
			if(locations.size() == 0 )
				throw new RuntimeException("archivo no define tag locations");			

			locations = UtilDOM.searchNode(locations.get(0), "location");
			if(locations.size() == 0 )
				throw new RuntimeException("no se ha definido ninguna location válida");
			
			urls	= new String[locations.size()];
			int i=0;
			for(Node loc:locations){
				url = UtilDOM.getAttributeValue(loc, "uri");
				if(url != null){
					url = context.getRealPath(url);
					//System.out.println(url);
					urls[i] = url;
					i++;
				}
			}
			
		} catch (Throwable e) {
			throw new RuntimeException("Error leyendo requestable.xml, "+ e.getMessage());
		}
		return urls;
	}
	private static Object enumToString(Enumeration<String> keys) {
		StringBuilder strb = new StringBuilder();
		while(keys.hasMoreElements())
			strb.append(keys.nextElement()).append(", ");
		return strb.toString();
	}
	private static void registerDuplicate(Hashtable<String, List<String>> duplicates,String identifier,String name){
		List<String> list;
		if(duplicates.containsKey(identifier)){
			list = duplicates.get(identifier); 
		}else{
			list = new ArrayList<String>();
			duplicates.put(identifier, list);
		}
		list.add(name);
	}
	private static boolean checkDuplicate(Hashtable<String, List<String>> duplicates){
		List<String> list;
		Enumeration<String> identifiers = duplicates.keys();
		String identifier;
		boolean hasDuplicate = false;
		while(identifiers.hasMoreElements()){
			identifier = identifiers.nextElement();
			list = duplicates.get(identifier);
			if(list.size() > 1){
				hasDuplicate = true;
				System.err.println(String.format("[ServletUtil] IDENTIFIER: %s duplicate on:",identifier));
				for(String c:list)
					System.err.println(String.format("\t\t %s",c));
			}else
				duplicates.remove(identifier);
		}
		return hasDuplicate;
	}
	
	public static String getServerAddress(HttpServletRequest request) throws UnknownHostException{

		StringBuilder strb = new StringBuilder();
		if(request.isSecure())
			strb.append("https://");
		else
			strb.append("http://");

		//append id
		//strb.append(InetAddress.getLocalHost().getHostAddress()).append(":");
		//append port
		//strb.append(request.getLocalPort());
		String host = request.getHeader("host");
		if(host == null || host.equals("")){
			host = InetAddress.getLocalHost().getHostAddress()+":"+request.getLocalPort();
		}
		//append host
		strb.append(host);
		//append App Context
		strb.append(request.getServletContext().getContextPath());
		return strb.toString();
	}

	public static void printParameters(HttpServletRequest request){
		System.out.println("-----------------------------------PARAMETERS-----------------------------------");
		Enumeration<String> par = request.getParameterNames();
		while(par.hasMoreElements()){
			String param = par.nextElement();
			System.out.println(String.format("%s=%s", param,request.getParameter(param)));
		}
	}
	public static HashMap<String, String> getParameters(HttpServletRequest request){
		HashMap<String, String> params = new HashMap<String, String>();
		Enumeration<String> par = request.getParameterNames();
		while(par.hasMoreElements()){
			String param = par.nextElement();
			params.put(param,request.getParameter(param));
		}
		return params;
	}
	public static void printSession(HttpServletRequest request){
		System.out.println("-----------------------------------SESSION-----------------------------------");
		HttpSession s = request.getSession();
		System.out.println(String.format("session=%s",s.getId()));
		Enumeration<String> names = s.getAttributeNames();
		while(names.hasMoreElements()){
			String name = names.nextElement();
			System.out.println(String.format("%s=%s", name,s.getAttribute(name)));
		}
		
	}
	public static void printHeaders(HttpServletRequest request,OutputStream output){
		PrintStream out = new PrintStream(output);
		out.println("-----------------------------------HEADERS-----------------------------------");
		
		Enumeration<String> names = request.getHeaderNames();
		while(names.hasMoreElements()){
			String name = names.nextElement();
			out.println(String.format("%s: %s", name,request.getHeader(name)));
		}
		
	}
	public static void printHeaders(HttpServletRequest request){
		printHeaders(request,System.out);
	}
	public static void readInputStream(HttpServletRequest request) throws IOException{
		System.out.println("-----------------------------------INPUT STREAM-----------------------------------");

		ServletInputStream inputStream = request.getInputStream();
		ByteArrayOutputStream bytes = StreamUtil.readInputStream(inputStream);
		System.out.println(bytes.toString());
	}
	
}
