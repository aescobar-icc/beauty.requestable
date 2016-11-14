package cl.beauty.requestable.exceptions;

import cl.beauty.util.reflection.UtilReflection;

public class RequestableException extends Exception {
	public static final int REQUEST_ALREADY_ON_PROCESS = 1000000001;
	/**
	 * 
	 */
	private static final long serialVersionUID = -1431402015452625836L;

	private int code;
	private String message;
	public RequestableException(String message) {
		super(message);
	}
	public RequestableException(String message,int code) {
		super(message);
		this.code = code;
	}
	public RequestableException(String message, Throwable cause) {
		super(message, cause);
		
		Throwable rootCause = UtilReflection.getRootCause(cause);
		String detail="";
		if(RequestableException.class.isAssignableFrom(rootCause.getClass()))
			detail = cause.getMessage();
		else
			detail = cause.toString();
		this.message = super.getMessage()+ " Detalle: "+ detail;
	}
	public int getCode() {
		return code;
	}
	@Override
	public String getMessage() {
		if(this.message == null)
			return super.getMessage();
		return this.message;
	}
	

	
}
