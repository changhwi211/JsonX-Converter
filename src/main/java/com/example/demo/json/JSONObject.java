package com.example.demo.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JSONObject {
	static final int OBJECT = 0;
	static final int ARRAY = 1;
	static final int TEXT = 2;
	
	private String name;
	private Map<String,String> attrMap;
	private String value;
	private int valueType;
	private List<JSONObject> childJSONObjList;
	
	public JSONObject(String name, String value, int valueType) {
		this.name = name;
		this.value = value;
		this.valueType = valueType;
		attrMap = new LinkedHashMap<String,String>();
		childJSONObjList = new ArrayList<JSONObject>();
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public int getValueType() {
		return valueType;
	}
	public void setValueType(int valueType) {
		this.valueType = valueType;
	}
	
	public void addChildJSONObj(JSONObject jsonObject) {
		childJSONObjList.add(jsonObject);
	}
	
	public void addAttr(String key, String value) {
		attrMap.put(key, value);
	}

	public List<JSONObject> getChildJSONObjList() {
		return childJSONObjList;
	}
	
	public String getAttrMapStr() {
		String attrMapStr = "";
		String key;
        String value;
		Iterator<String> mapIter = attrMap.keySet().iterator();
		
		while(mapIter.hasNext()){
            key = mapIter.next();
            value = attrMap.get(key);
            attrMapStr += " " + key + "=\"" + value + "\""; 
        }
		
		return attrMapStr;
	}
	
	public boolean isHaveAttrMap() {
		if(attrMap.size() >= 1) {
			return true;
		}else {
			return false;
		}
	}
}
