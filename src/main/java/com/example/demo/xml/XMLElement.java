package com.example.demo.xml;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class XMLElement {
	//elements의 타입
	static final public int ELEMENT = 0;//태그 내부 엘리먼트가 단순 텍스트일 경우
	static final public int TEXT = 1;//태그 내부 엘리먼트에 다른 태그가 포함된 경우
	
	private String name;//태그이름
	private Map<String,String> attributesMap;//태그 내 속성
	private String elements;//태그 내부 엘리먼트 문자열
	private int elementType;//태그 내부 엘리먼트 문자열의 타입(ELEMENT or TEXT)
	private boolean haveAttributes;//속성의 존재 여부
	
	public XMLElement(String name, String attributesStr, String elements) {
		this.name = name;
		this.elements = elements;
		setHaveAttributes(attributesStr);
		setAttributesMap(attributesStr);
		setElementType(elements);
	}
	
	public String getName() {
		return name;
	}
	
	public Map<String, String> getAttributesMap() {
		return attributesMap;
	}
	
	//태그 속성 문자열을 입력받아 속성 Map으로 변환시켜주는 함수
	private void setAttributesMap(String attributesStr) {
		int idx = 0;
		int startIdx = 0;
		int endIdx = 0;
		String key = null;
		String value = null;
		
		if(attributesStr != null) {
			this.attributesMap = new LinkedHashMap<String,String>();
			while((idx=attributesStr.indexOf("=")) != -1) {
				key = XMLUtils.checkNameSpace(attributesStr.substring(0,idx).trim());
				if(attributesStr.substring(idx+1).trim().charAt(0)=='\"') {
					startIdx = attributesStr.indexOf("\"");
					endIdx = attributesStr.indexOf("\"",startIdx+1);
					value = attributesStr.substring(startIdx+1,endIdx);
				}else {
					startIdx = attributesStr.indexOf("\'");
					endIdx = attributesStr.indexOf("\'",startIdx+1);
					value = attributesStr.substring(startIdx+1,endIdx);
				}
				value = XMLUtils.changeToJSONString(value);
				this.attributesMap.put(key, value);
				attributesStr = attributesStr.substring(endIdx+1).trim();
			}
		}else {
			this.attributesMap = null;
		}
	}
	
	public String getElements() {
		return elements;
	}
	
	//태그 내부 엘리먼트 문자열의 타입(ELEMENT or TEXT)을 설정하는 함수
	private void setElementType(String elements) {
		if(elements.indexOf("<") != -1) {
			this.elementType = ELEMENT;
		}else {
			this.elementType = TEXT;
		}
	}
	
	public int getElementType() {
		return elementType;
	}
	
	public boolean isHaveAttributes() {
		return haveAttributes;
	}
	
	//속성 포함 여부를 설정하는 함수
	private void setHaveAttributes(String attributesStr) {
		if(attributesStr != null) {
			this.haveAttributes = true; 
		}else {
			this.haveAttributes = false;	
		}
	}
	
	
}
