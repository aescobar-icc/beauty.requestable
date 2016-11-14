package cl.beauty.requestable;

import java.util.ArrayList;
import java.util.List;

import cl.beauty.util.serialize.JSONUtil;

/**
 * This class implements the response struct for an requestable service
 * @author aescobar
 *
 */
public class RequestableResponse {
	private Object			data;
	private List<String>	messages;
	private String			result = "FAIL";
	private int				resultCode = 0;
	
	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}
	public List<String> getMessages() {
		return messages;
	}
	public void addMessage(String message) {
		if(this.messages == null)
			this.messages = new ArrayList<String>();
		this.messages.add(message);
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	public int getResultCode() {
		return resultCode;
	}
	public void setResultCode(int resultCode) {
		this.resultCode = resultCode;
	}
	
	public String parseJson(){
		return JSONUtil.encodeJsonString(this);
	}
	

}
