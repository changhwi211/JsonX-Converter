package com.example.demo.json;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.demo.err.InvalidKeyOfJSONException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONUtils {
	//입력받은 JSON 문자열이 유효한지 확인하는 함수
	public static boolean isJSONValid(String jsonInString) {
		try {
			final ObjectMapper mapper = new ObjectMapper();
			mapper.readTree(jsonInString);
			System.out.println("right json");
			return true;
		} catch (IOException e) {
			System.out.println("wrong json");
			return false;
		}
	}

	//입력받은 JSON 문자열을 XML 문자열로 변환 하는 함수
	public static String convertJSONToXML(String json) {
		JSONObject jsonObject = getJSONObject(json);
		
		String xml = getXMLString(jsonObject);

		return xml;
	}

	//JSONObject를 XML 문자열로 변환하는 함수
	private static String getXMLString(JSONObject jsonObject) {
		StringBuffer sb = new StringBuffer();
		String attr = "";
		
		//jsonObject에 속성이 존재할 경우 속성을 문자열로 얻음
		if(jsonObject.isHaveAttrMap()) {
			attr = jsonObject.getAttrMapStr();
		}
		
		//시작 태그
		sb.append("<" + jsonObject.getName() + attr + ">");
		
		//jsonObject의 value 타입이 TEXT이면 자식 JSONObject List가 존재하지 X
		if(jsonObject.getValueType() == JSONObject.TEXT) {
			sb.append(jsonObject.getValue());//TEXT를 append
		}else {//자식 JSONObject List를 XML 문자열로 구성한 값(재귀 호출)을 append 
			sb.append("\r\n" + getXMLString(jsonObject.getChildJSONObjList(), 1));
		}
		
		//끝 태그
		sb.append("</" + jsonObject.getName() + ">");
		
		return sb.toString();
	}
	
	//자식 JSONObject List를 XML 문자열로 변환하는 함수(인자로 받은 계층 만큼 tab 삽입)
	private static String getXMLString(List<JSONObject> jsonObjectList, int seq) {
		StringBuffer sb = new StringBuffer();
		String xml = "";
		
		//자식 JSONObject List의 모든 요소에 대해 반복
		for(JSONObject jsonObject : jsonObjectList) {
			String attr = "";
			
			if(jsonObject.isHaveAttrMap()) {
				attr = jsonObject.getAttrMapStr();
			}
			
			//시작 태그(계층 만큼 tab 삽입)
			sb.append(getTABString(seq) + "<" + jsonObject.getName() + attr + ">");
			
			if(jsonObject.getValueType() == JSONObject.TEXT) {
				sb.append(jsonObject.getValue());
			}else {
				sb.append("\r\n" + getXMLString(jsonObject.getChildJSONObjList(), seq+1) + getTABString(seq));
			}
			
			//끝 태그
			sb.append("</" + jsonObject.getName() + ">\r\n");
			
		}
		
		return sb.toString();
	}
	
	//계층만큼 tab 출력 함수
	private static String getTABString(int seq) {
		String tabStr = "";
		for(int i=0;i<seq;i++) {
			tabStr += "\t";
		}
		return tabStr;
	}

	//JSON 문자열을 JSONObject로 재구성하는 함수
	private static JSONObject getJSONObject(String json)  {
		JSONObject jsonObject;
		int closeIdx = 0;
		String name = "root";//XML로 변환 되었을 때 XML 최상위 엘리먼트가 존재하지 않을 경우를 대비한 최상위 엘리먼트 태그 이름
		String value;
		String regEx = "[^\\\\]\"";//string의 닫는 더블쿼테이션(")을 찾기위한 정규식
		Pattern pat = Pattern.compile(regEx);
		Matcher m;

		//JSON 문자열에 존재하는 모든 공백 제거
		json = json.replaceAll("(\r\n|\r|\n|\n\r|\t)", "").trim();

		char ch = json.charAt(0);

		if (ch == '{') {//OBJECT(Map)일 경우
			closeIdx = getCloseIdx(json, ch);//'}'의 인덱스
			value = json.substring(1, closeIdx).trim();
			jsonObject = new JSONObject(name, value, JSONObject.OBJECT);//JSONObject 생성
			makeJSONObject(jsonObject);//JSONObject 트리 구성
		} else if (ch == '[') {//JSON ARRAY일 경우
			closeIdx = getCloseIdx(json, ch);//']'의 인덱스
			value = json.substring(1, closeIdx).trim();
			jsonObject = new JSONObject(name, value, JSONObject.ARRAY);
			makeJSONObject(jsonObject);
		} else if (ch == '"') {//String일 경우
			m = pat.matcher(json);
			if(m.find(0)) {
				//string의 닫는 더블 쿼테이션(")의 앞에 "\"이 아닌 문자가 오기 때문에 결과 인덱스에 1을 더해준다
				closeIdx = m.start()+1;
			}
			value = changeToXMLString(json.substring(1, closeIdx));
			jsonObject = new JSONObject(name, value, JSONObject.TEXT);
		} else {//그 외의 경우
			value = json.trim();
			jsonObject = new JSONObject(name, value, JSONObject.TEXT);
		}
		
		//자식 JSONObject List의 개수가 1개이면 XML 최상위 엘리먼트가 이미 존재하는 것임
		if (jsonObject.getChildJSONObjList().size() == 1) {
			//임의로 붙인 root 태그 제거를 위해 JSONObject 추출
			jsonObject = jsonObject.getChildJSONObjList().get(0);
		}

		return jsonObject;
	}

	//JSONObject 트리를 구성하는 함수
	private static void makeJSONObject(JSONObject jsonObject)  {
		String regEx = "[^\\\\]\"";//string의 닫는 더블쿼테이션(")을 찾기위한 정규식
		Pattern pat = Pattern.compile(regEx);
		Matcher m;
		
		String parentValue = jsonObject.getValue();
		
		//value가 빈 문자열일 경우
		if (parentValue.isEmpty()) {
			jsonObject.setValue("");
			jsonObject.setValueType(JSONObject.TEXT);//jsonObject의 타입을 TEXT로 변경
			return;
		}
		//jsonObject의 타입이 OBJECT(Map)일 경우
		if (jsonObject.getValueType() == JSONObject.OBJECT) {
			JSONObject childJsonObject;
			int tmpIdx = 0;
			int openIdx = 0;
			int closeIdx = 0;
			String key;
			String value;
			
			//OBJECT(Map)의 모든 요소에 대해서 반복
			while (true) {
				boolean isAttrKey = false;
				boolean isAttrValue = false;
	
				openIdx = parentValue.indexOf("\"");
				
				m = pat.matcher(parentValue);
				if(m.find(openIdx)) {
					closeIdx = m.start()+1;
				}
				
				key = parentValue.substring(openIdx + 1, closeIdx);//key 추출
				
				//key가 '-'로 시작할 경우
				if(key.charAt(0) == '-') {
					tmpIdx = parentValue.indexOf(":", closeIdx + 1);
					//key에 대한 value가 TEXT 타입일 경우 XML의 속성이다
					if(parentValue.substring(tmpIdx + 1).trim().charAt(0)=='"')
						isAttrKey = true;
				}else if(key.equals("#text")) {//key가 #text일 경우
					tmpIdx = parentValue.indexOf(":", closeIdx + 1);
					//key에 대한 value가 TEXT 타입일 경우 XML의 텍스트 엘리먼트임
					if(parentValue.substring(tmpIdx + 1).trim().charAt(0)=='"')
						isAttrValue = true;
				}
				
				if (isAttrKey) {//XML의 속성이면
					key = changeToXMLString(checkKey(key.substring(1)));
					openIdx = parentValue.indexOf("\"", closeIdx + 1);
					
					m = pat.matcher(parentValue);
					if(m.find(openIdx)) {
						closeIdx = m.start()+1;
					}
					
					value = changeToXMLString(parentValue.substring(openIdx + 1, closeIdx));
					
					jsonObject.addAttr(key, value);//jsonObject에 속성 추가
				} else if (isAttrValue) {//XML의 텍스트 엘리먼트 이면
					openIdx = parentValue.indexOf("\"", closeIdx + 1);
					
					m = pat.matcher(parentValue);
					if(m.find(openIdx)) {
						closeIdx = m.start()+1;
					}
					
					value = changeToXMLString(parentValue.substring(openIdx + 1, closeIdx));
			
					jsonObject.setValue(value);
					jsonObject.setValueType(JSONObject.TEXT);//jsonObject의 타입을 OBJECT에서 TEXT로 변경
				} else {
			
					key = changeToXMLString(checkKey(key));
			
					tmpIdx = parentValue.indexOf(":", closeIdx + 1);
					parentValue = parentValue.substring(tmpIdx + 1).trim();

					char ch = parentValue.charAt(0);

					if (ch == '{') {//key에 대한 value가 OBJECT(Map)이면
						closeIdx = getCloseIdx(parentValue, ch);
						value = parentValue.substring(1, closeIdx).trim();
						childJsonObject = new JSONObject(key, value, JSONObject.OBJECT);//자식 JSONObject 생성
						makeJSONObject(childJsonObject);//자식 JSONObject에 대해 JSONObject 트리 구성(재귀 호출)
						jsonObject.addChildJSONObj(childJsonObject);//위에서 JSONObject 트리로 구성된 자식 JSONObject를 jsonObject의 자식 리스트에 포함
					} else if (ch == '[') {//key에 대한 value가 ARRAY이면
						String subParentValue;

						closeIdx = getCloseIdx(parentValue, ch);
						subParentValue = parentValue.substring(1, closeIdx).trim();//ARRAY의 내부를 추출
						if (subParentValue.isEmpty()) {//빈 문자열일 경우
							childJsonObject = new JSONObject(key, "", JSONObject.TEXT);
							jsonObject.addChildJSONObj(childJsonObject);
						} else {
							int subCloseIdx = 0;
							//ARRAY 내부의 모든 요소를 JSONObject 트리로 구성하여 jsonObject의 자식 리스트에 추가
							while (true) {
								ch = subParentValue.charAt(0);
								if (ch == '{') {
									subCloseIdx = getCloseIdx(subParentValue, ch);
									value = subParentValue.substring(1, subCloseIdx).trim();
									childJsonObject = new JSONObject(key, value, JSONObject.OBJECT);
									makeJSONObject(childJsonObject);
									jsonObject.addChildJSONObj(childJsonObject);
								} else if (ch == '[') {
									subCloseIdx = getCloseIdx(subParentValue, ch);
									value = subParentValue.substring(1, subCloseIdx).trim();
									childJsonObject = new JSONObject(key, value, JSONObject.ARRAY);
									makeJSONObject(childJsonObject);
									jsonObject.addChildJSONObj(childJsonObject);
								} else if (ch == '"') {
									openIdx = subParentValue.indexOf("\"");
									
									m = pat.matcher(subParentValue);
									if(m.find(openIdx)) {
										subCloseIdx = m.start()+1;
									}
									value = changeToXMLString(subParentValue.substring(openIdx + 1, subCloseIdx));
									childJsonObject = new JSONObject(key, value, JSONObject.TEXT);
									jsonObject.addChildJSONObj(childJsonObject);
								} else {
									if ((tmpIdx = subParentValue.indexOf(",")) != -1) {
										value = subParentValue.substring(0, tmpIdx).trim();
										childJsonObject = new JSONObject(key, value, JSONObject.TEXT);
										jsonObject.addChildJSONObj(childJsonObject);
										subCloseIdx = tmpIdx - 1;
									} else {
										value = subParentValue.trim();
										childJsonObject = new JSONObject(key, value, JSONObject.TEXT);
										jsonObject.addChildJSONObj(childJsonObject);
										break;
									}
								}
								if ((tmpIdx = subParentValue.indexOf(",", subCloseIdx + 1)) != -1) {
									subParentValue = subParentValue.substring(tmpIdx + 1).trim();
								} else {
									break;
								}
							}
						}
					} else if (ch == '"') {//key에 대한 value가 String이면
						openIdx = parentValue.indexOf("\"");
						
						m = pat.matcher(parentValue);
						if(m.find(openIdx)) {
							closeIdx = m.start()+1;
						}
						
						value = changeToXMLString(parentValue.substring(openIdx + 1, closeIdx));
						childJsonObject = new JSONObject(key, value, JSONObject.TEXT);
						jsonObject.addChildJSONObj(childJsonObject);
					} else {//key에 대한 value가 그 외의 것이면
						if ((tmpIdx = parentValue.indexOf(",")) != -1) {
							value = parentValue.substring(0, tmpIdx).trim();
							childJsonObject = new JSONObject(key, value, JSONObject.TEXT);
							jsonObject.addChildJSONObj(childJsonObject);
							closeIdx = tmpIdx - 1;
						} else {
							value = parentValue.trim();
							childJsonObject = new JSONObject(key, value, JSONObject.TEXT);
							jsonObject.addChildJSONObj(childJsonObject);
							break;
						}
					}
				}

				if ((tmpIdx = parentValue.indexOf(",", closeIdx + 1)) != -1) {
					parentValue = parentValue.substring(tmpIdx + 1).trim();
				} else {
					break;
				}

			}
		} else if (jsonObject.getValueType() == JSONObject.ARRAY) {//jsonObject의 타입이 ARRAY일 경우
			JSONObject childJsonObject;
			int tmpIdx;
			int openIdx;
			int closeIdx = 0;
			String name = "element";//ARRAY는 키가 없으므로 element로 지정함
			String value;
			
			//ARRAY 내부의 모든 요소를 JSONObject 트리로 구성하여 jsonObject의 자식 JSONObject로 추가
			while (true) {
				char ch = parentValue.charAt(0);
				if (ch == '{') {
					closeIdx = getCloseIdx(parentValue, ch);
					value = parentValue.substring(1, closeIdx).trim();
					childJsonObject = new JSONObject(name, value, JSONObject.OBJECT);
					makeJSONObject(childJsonObject);
					jsonObject.addChildJSONObj(childJsonObject);
				} else if (ch == '[') {
					closeIdx = getCloseIdx(parentValue, ch);
					value = parentValue.substring(1, closeIdx).trim();
					childJsonObject = new JSONObject(name, value, JSONObject.ARRAY);
					makeJSONObject(childJsonObject);
					jsonObject.addChildJSONObj(childJsonObject);
				} else if (ch == '"') {
					openIdx = parentValue.indexOf("\"");
					
					m = pat.matcher(parentValue);
					if(m.find(openIdx)) {
						closeIdx = m.start()+1;
					}
					
					value = changeToXMLString(parentValue.substring(openIdx + 1, closeIdx));
					childJsonObject = new JSONObject(name, value, JSONObject.TEXT);
					jsonObject.addChildJSONObj(childJsonObject);
				} else {
					if ((tmpIdx = parentValue.indexOf(",")) != -1) {
						value = parentValue.substring(0, tmpIdx).trim();
						childJsonObject = new JSONObject(name, value, JSONObject.TEXT);
						jsonObject.addChildJSONObj(childJsonObject);
						closeIdx = tmpIdx - 1;
					} else {
						value = parentValue.trim();
						childJsonObject = new JSONObject(name, value, JSONObject.TEXT);
						jsonObject.addChildJSONObj(childJsonObject);
						break;
					}
				}
				
				if ((tmpIdx = parentValue.indexOf(",", closeIdx + 1)) != -1) {
					parentValue = parentValue.substring(tmpIdx + 1).trim();
				} else {
					break;
				}
			}
		}
	}
	
	//JSON OBJECT의 '}' 또는 JSON ARRAY의 ']'의 인덱스 반환 함수
	private static int getCloseIdx(String str, char ch) {
		String strRegEx = "[^\\\\]\"";//string의 닫는 더블쿼테이션(")을 찾기위한 정규식
		Pattern strPat = Pattern.compile(strRegEx);
		Matcher mStr = strPat.matcher(str);
		
		String regEx;
		Pattern pat;
		Matcher m;
		
		int closeIdx = 0;
		int strCloseIdx = -1;

		if (ch == '{') {
			regEx = "[{}\"]";
		} else if (ch == '[') {
			regEx = "[\\[\\]\"]";
		} else {
			return -1;
		}

		pat = Pattern.compile(regEx);
		m = pat.matcher(str);
		
		int count = 0;

		while (m.find()) {
			
			if(m.start()>strCloseIdx) {
				if(m.group().equals("\"")) {
					if(mStr.find(m.start())) {
						//string의 닫는 더블 쿼테이션(")의 앞에 "\"이 아닌 문자가 오기 때문에 결과 인덱스에 1을 더해준다
						strCloseIdx = mStr.start()+1;
					}
				}else if(m.group().equals(""+ch)) {
					count++;
				}else {
					count--;
				}
			}else {
				continue;
			}
			
			if (count == 0) {
				closeIdx = m.start();
				break;
			}
		}
		
		return closeIdx;
	}
	
	//XML에서 충돌될수 있는 문자 변경, 문자 => 제어문자로 변경
	public static String changeToXMLString(String str) {
		str = str.replace("&","&amp;").replace("<","&lt;").replace(">", "&gt;").replace("'", "&apos;")
				.replace("\\\"", "&quot;").replace("\\\\","\\").replace("\\/","/").replace("\\b","\b")
				.replace("\\f","\f").replace("\\n","\n").replace("\\r","\r").replace("\\t","\t");
		return str;
	}
	
	//XML로 변환 되었을 때, XML 태그 이름 문법에 저촉되는지 확인하는 함수
	public static String checkKey(String key) {
		String regEx = "[\\t\\n\\r!\\?,#@%&;<>\\(\\)'\"`\\{\\}/\\[\\]\\\\^\\$\\|~\\*\\+=:\\s]";
		Pattern pat = Pattern.compile(regEx);;
		Matcher m = pat.matcher(key);
		
		if(m.find()) {
			if(!m.group().equals(" ")) {
				throw new InvalidKeyOfJSONException("key 내부에 '" + m.group() + "'를 포함할 수 없습니다.");
			}else {
				throw new InvalidKeyOfJSONException("key 내부에 공백문자를 포함할 수 없습니다.");
			}
			
		}else if(key.charAt(0) == '-') {
			throw new InvalidKeyOfJSONException("key는 '-'로 시작할 수 없습니다.");
		}else if(key.charAt(0) == '.') {
			throw new InvalidKeyOfJSONException("key는 '.'로 시작할 수 없습니다.");
		}
		
		return key;
	}

}
