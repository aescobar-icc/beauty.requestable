package cl.beauty.util.serialize;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import cl.beauty.util.date.DateTime;
import cl.beauty.util.reflection.UtilReflection;
import cl.beauty.util.serialize.annotations.JSONNonSerializableClass;
import cl.beauty.util.serialize.annotations.JSONNonSerializableField;

public class JSONUtil {
	private static final String TO_PACKAGE = "cl.beauty.negocio.to.";
	/**
	 * 
	 * @deprecated Do not use this method!, <strong>use decodeList instead </strong>
	 */
	@Deprecated
	public static List<?> parseList(String jsonString, Class<?> type) throws Exception {
		return decodeList(jsonString, type);
	}

	public static String encodeJsonString(Object object){
			List<Object> serialized = new ArrayList<Object>();
			Object o = encodeJsonObject(object,serialized);

			if(o instanceof JSONObject)
				return ((JSONObject)o).toJSONString();
			if(o instanceof JSONArray)
				return ((JSONArray)o).toJSONString();
			
			return String.valueOf(o);
	}
	public static <T> List<T> decodeList(String jsonArrayString,Class<T> listPararameterType) throws JSONException {
		try{
			if(jsonArrayString == null || jsonArrayString.equals(""))
				return null;
			
			List<T>		list	= new ArrayList<T>();
			JSONParser	parser	= new JSONParser();
			JSONArray	array	= (JSONArray)parser.parse(jsonArrayString);
			for(Object jObj:array){
				T instance = parseObject(jObj, listPararameterType);
				list.add(instance);
			}		
			return list;
		}catch(Throwable e){
			throw new JSONException(String.format("Error: decodeList of %s",listPararameterType.getName()), e);
		}
	}
	@SuppressWarnings("unchecked")
	public static <T> T decodeObject(String jsonObjectString, Class<T> cls) throws JSONException{
		try{
			if(jsonObjectString == null)
				return null;
			if(jsonObjectString.equals("")){
				if(String.class.isAssignableFrom(cls))
					return (T) jsonObjectString;
				return null;
			}
	
			if(UtilReflection.isNativeType(cls))
				return UtilReflection.parseToNative(cls, jsonObjectString);
			
			JSONParser parser 	= new JSONParser();
			Object obj = parser.parse(jsonObjectString);
			if(obj == null)
				return null;
			
			JSONObject jsonObj	= (JSONObject)obj;
			return parseObject(jsonObj, cls);
		}catch(Throwable e){
			throw new JSONException(String.format("Error: decodeObject %s",cls.getName()), e);
		}
	}
	/**
	 * 
	 * @param jsonObj
	 * @param type
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private static <T> T parseObject(Object obj,Class<T> type) throws Exception{
		if(UtilReflection.NATIVE_TYPES.contains(type) || UtilReflection.WRAPPER_NATIVE_TYPES.contains(type)){
			return  UtilReflection.parseToNative(type, String.valueOf(obj));
		}else{
			JSONObject jsonObj = (JSONObject)obj;
			if(jsonObj.get("jsonCompatibleType") != null){
				Class<?> compatipleType =  Class.forName(TO_PACKAGE+(String)jsonObj.get("jsonCompatibleType"));
				if(type.isAssignableFrom(compatipleType)){
					type = (Class<T>) compatipleType;
				}
			}
			T instance = type.newInstance();
			for(Object fieldName: jsonObj.keySet()){
				try{
					if(fieldName.equals("serialVersionUID"))
							continue;
					Field field = UtilReflection.getField((String)fieldName, type);
					setField(instance, field, jsonObj.get(fieldName));
				}catch(NoSuchFieldException e){
					//System.err.println(String.format("[JSONUtil.parseList] Field:%s does no exists in %s",fieldName,type));
				}catch(Throwable e){
					Throwable cause = UtilReflection.getRootCause(e);
					System.err.println(String.format("[JSONUtil.parseList] ERROR parsing field:%s caused by: %s",fieldName,cause.toString()));
				}
			}
			return instance;
		}
	}
	private static void setField(Object instance,Field field,Object value) throws Exception{
		//System.out.println(String.format("parsing %s=%s --> %s", field.getName(),value,field.getType()));
		if(value == null){
			UtilReflection.setFieldValue(field, instance, null);
		}else{
			Class<?> type = field.getType();
			if(Collection.class.isAssignableFrom(type)){
				Type parameterType = UtilReflection.getParameterTypes(field)[0];
				Collection<Object> list = new ArrayList<Object>();
				JSONArray array =(JSONArray)value;
				for(Object jObj:array){
					Object object = parseObject(jObj, (Class<?>) parameterType);
					list.add(object);
				}
				UtilReflection.setFieldValue(field, instance, list);
			}else if(Map.class.isAssignableFrom(type)){
				//return encodeJsonMap((Map<?, ?>)object);
			}else if(Enum.class.isAssignableFrom(type)){
				//return createJSONObject(object);
			}else{
				UtilReflection.setFieldValue(field, instance, parseObject(value,field.getType()));
			}
		}
	}
	@SuppressWarnings("unchecked")
	private static JSONArray encodeJsonList(Collection<?> coll,List<Object> serialized){
		JSONArray list = new JSONArray();
		for(Object o:coll){
			list.add(encodeJsonObject(o,serialized));
		}
		return list;
	}
	@SuppressWarnings("unchecked")
	private static JSONArray encodeJsonArray(Object[] array,List<Object> serialized){
		JSONArray list = new JSONArray();
		for(Object o:array){
			list.add(encodeJsonObject(o,serialized));
		}
		return list;
	}
	@SuppressWarnings("unchecked")
	private static JSONObject encodeJsonMap(Map<?, ?> map,List<Object> serialized){
		JSONObject obj = new JSONObject();	
		Object o;
		for(Object key:map.keySet()){
			o = map.get(key);
			obj.put(key,encodeJsonObject(o,serialized));
		}
		return obj;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject createJSONObject(Object object,List<Object> serialized){
		JSONObject obj=new JSONObject();
		List<Field> fields;
		if(Throwable.class.isAssignableFrom(object.getClass()))
			fields = Arrays.asList(object.getClass().getDeclaredFields());
		else
			fields = UtilReflection.getAllFields(object.getClass(),true);
		for(Field field:fields){
			try{
				if(field.isAnnotationPresent(JSONNonSerializableField.class))
					continue;//ignora los campos anotados
				if (field.getType().isAssignableFrom(char.class))
					obj.put(field.getName() ,String.valueOf(UtilReflection.getFieldValue(field, object)));
				else
					obj.put(field.getName() ,encodeJsonObject(UtilReflection.getFieldValue(field, object),serialized));	
					
			}catch(Throwable e){
				obj.put(field.getName(),null);
				//System.out.println(String.format("[JSONUtil.encodeJsonObject] Imposible encode %s.%s",object.getClass().getSimpleName(),field.getName()));
			}
		}
		return obj;
	}
	private static Object encodeJsonObject(Object object,List<Object> serialized){
		if(object != null && !object.getClass().isAnnotationPresent(JSONNonSerializableClass.class) && !serialized.contains(object)){
			if(Collection.class.isAssignableFrom(object.getClass())){
				return encodeJsonList((Collection<?>)object,serialized);
			}else if(object.getClass().isArray()){
				return encodeJsonArray((Object[]) object,serialized);
			}else if(Map.class.isAssignableFrom(object.getClass())){
				return encodeJsonMap((Map<?, ?>)object,serialized);
			}else if(Enum.class.isAssignableFrom(object.getClass())){
				return createJSONObject(object,serialized);
			}else{
	
				if(UtilReflection.isPosibleNativeType(object.getClass())){
					if(		object.getClass() == DateTime.class
							|| object.getClass() == java.sql.Date.class 
							|| object.getClass() == java.util.Date.class 
							|| object.getClass() == java.sql.Timestamp.class 
							|| object.getClass() == java.sql.Time.class 
							|| object.getClass() == StackTraceElement.class)
						return String.valueOf(object);
					else 
						return object;
				}
				serialized.add(object);
				return createJSONObject(object,serialized);
			}
		}
		return null;
	}
}
