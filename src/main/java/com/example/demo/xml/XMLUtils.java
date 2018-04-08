package com.example.demo.xml;

import java.util.ArrayList;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import com.example.demo.err.InvalidNameSpaceOfXMLException;

public class XMLUtils {
	//XML을 JSON Map으로 변환하는 함수
	static public Map<String, Object> convertXMLToJSON(String xml) {
		Map<String, Object> jsonMap = new LinkedHashMap<String, Object>();
		XMLElement rootElement;

		//최상위 엘리먼트의 정보를 담은 XMLElement 객체(rootElement)를 추출
		rootElement = parseXML(xml).get(0);

		//최상위 엘리먼트의 태그 이름을 Key로, rootElement를 Map에 포함될 Object로 구성하여 Value로 설정한 후 Map에 등록 
		jsonMap.put(rootElement.getName(), makeElementsObj(rootElement));

		return jsonMap;//최종 JSON Map 반환

	}

	//XML 문자열의 가장 상위 엘리먼트들을 파싱하여 List로 반환하는 함수
	static private List<XMLElement> parseXML(String xml) {
		List<XMLElement> list = new ArrayList<XMLElement>();
		XMLElement xmlElement;

		int startTagFirstIdx = 0;
		int startTagLastIdx = 0;
		int endTagFirstIdx = 0;
		int endTagLastIdx = 0;

		String startTagVal;
		String tagName;
		String tagElements;
		String tagAttributes;

		String regEx = "<[^\\?/]";//xml 문자열 내에서 첫 태그를 찾기 위한 정규식
		Pattern pat = Pattern.compile(regEx);
		String endTagRegEx;
		Pattern endTagPat;
		String blankRegEx = "(\r\n|\r|\n|\n\r|\t|\\s)";//태그 이름과 속성을 구분하는 문자의 인덱스를 찾기 위한 정규식
		Pattern blankPat = Pattern.compile(blankRegEx);
		Matcher m;

		xml = xml.replaceAll("(<!--).*(-->)", "");//xml 문자열 내에서 주석 제거
		xml = xml.trim();//xml 문자열의 앞 뒤 공백 제거

		//xml 문자열 내 태그들 파싱
		while (!xml.equals("") && pat.matcher(xml).find()) {
			startTagVal = null;
			tagName = null;
			tagElements = null;
			tagAttributes = null;

			m = pat.matcher(xml);

			if (m.find()) {
				startTagFirstIdx = m.start();//시작 태그 '<'의 인덱스
			}

			int idxOfXML = 0;
			int tmpStartTagLastIdx = 0;
			char ch;
			String tmpStartTagVal = null;

			tmpStartTagLastIdx = xml.indexOf(">", startTagFirstIdx + 1);// 시작 태그 '>'의 임시 인덱스
			tmpStartTagVal = xml.substring(startTagFirstIdx + 1, tmpStartTagLastIdx);

			//속성이 존재할 경우 => 현재 검색한 '>'가 속성 내의 문자 '>'일 가능성이 있다
			if (tmpStartTagVal.indexOf("=") != -1) {
				idxOfXML = startTagFirstIdx;

				do {
					idxOfXML = xml.indexOf("=", idxOfXML + 1);
					ch = xml.substring(idxOfXML + 1).trim().charAt(0);// " or '
					idxOfXML = xml.indexOf(ch, idxOfXML + 1);// 시작 " or '
					idxOfXML = xml.indexOf(ch, idxOfXML + 1);// 끝 " or '
					tmpStartTagLastIdx = xml.indexOf(">", idxOfXML + 1);
					tmpStartTagVal = xml.substring(idxOfXML + 1, tmpStartTagLastIdx).trim();
				} while (tmpStartTagVal.indexOf("=") != -1);

				startTagLastIdx = tmpStartTagLastIdx;//시작 태그 '>'의 인덱스
			} else {
				startTagLastIdx = tmpStartTagLastIdx;//시작 태그 '>'의 인덱스
			}

			startTagVal = xml.substring(startTagFirstIdx + 1, startTagLastIdx).trim();
			if (startTagVal.charAt(startTagVal.length() - 1) == '/') {// self_closing인 빈 엘리먼트일 경우 ex)<tag/>
				startTagVal = startTagVal.substring(0, startTagVal.length() - 1).trim();

				//태그 내에 속성이 존재할 경우 태그 이름과 속성을 분리
				if (startTagVal.indexOf("=") != -1) {
					m = blankPat.matcher(startTagVal);
					m.find();
					tagName = startTagVal.substring(0, m.start());
					tagAttributes = startTagVal.substring(m.start()).trim();
				} else {
					tagName = startTagVal;
				}

				tagElements = "";

				xml = xml.substring(startTagLastIdx + 1);
			} else {//시작 태그와 끝 태그가 모두 존재하는 경우 ex)<tag></tag>
				//태그내에 속성이 존재할 경우 태그 이름과 속성을 분리
				if (startTagVal.indexOf("=") != -1) {
					m = blankPat.matcher(startTagVal);
					m.find();
					tagName = startTagVal.substring(0, m.start());
					tagAttributes = startTagVal.substring(m.start()).trim();
				} else {
					tagName = startTagVal;
				}

				endTagRegEx = "(</" + tagName + ")\\s*>";//끝 태그를 찾기위한 정규식
				endTagPat = Pattern.compile(endTagRegEx);
				m = endTagPat.matcher(xml);

				if (m.find()) {
					endTagFirstIdx = m.start();//끝 태그 '<'의 인덱스
				}
				
				endTagLastIdx = xml.indexOf(">", endTagFirstIdx + 1);//끝 태그 '>'의 인덱스

				tagElements = xml.substring(startTagLastIdx + 1, endTagFirstIdx).trim();//태그 내부 엘리먼트 문자열 추출

				xml = xml.substring(endTagLastIdx + 1).trim();//xml 문자열에서 파싱된 태그 부분을 제외
			}
			
			tagName = checkNameSpace(tagName);
			//태그이름,속성 문자열,태그 내부 엘리먼트 문자열을 인자로 XMLElement 객체를 생성
			xmlElement = new XMLElement(tagName, tagAttributes, tagElements); 
			//XMLElement List에 추가
			list.add(xmlElement);
		}
		return list;//XMLElement List 리턴
	}

	//XMLElement를 JSON Map에 포함될 Object로 구성하는 함수
	static private Object makeElementsObj(XMLElement xmlElement) {
		//xmlElement를 Map으로 재구성하여 담을 변수(objMap)
		Map<String, Object> objMap = new LinkedHashMap<String, Object>();

		//xmlElement의 내부 엘리먼트 타입이 TEXT일 경우(재귀함수 최초 탈출 조건)
		if (xmlElement.getElementType() == XMLElement.TEXT) {
			if (!xmlElement.isHaveAttributes()) {//xmlElement에 속성이 존재하지 않을 경우
				return changeToJSONString(xmlElement.getElements());//xmlElement의 내부 엘리먼트 문자열(TEXT)를 반환
			} else {//xmlElement에 속성이 존재할 경우 Map을 구성
				Map<String, String> attributesMap = xmlElement.getAttributesMap();
				Iterator<String> iter = attributesMap.keySet().iterator();
				
				//xmlElement의 속성을  objMap에 추가
				while (iter.hasNext()) {
					String key = iter.next();
					String value = attributesMap.get(key);
					objMap.put("-" + key, value);
				}
				//xmlElement의 내부 엘리먼트 문자열(TEXT)를 objMap에 추가
				objMap.put("#text", changeToJSONString(xmlElement.getElements()));
			}
		} else {//xmlElement의 내부 엘리먼트 타입이 ELEMENT일 경우
			//xmlElement에 속성이 존재할 경우 objMap에 추가
			if (xmlElement.isHaveAttributes()) {
				Map<String, String> attributesMap = xmlElement.getAttributesMap();
				Iterator<String> iter = attributesMap.keySet().iterator();

				while (iter.hasNext()) {
					String key = iter.next();
					String value = attributesMap.get(key);
					objMap.put("-" + key, value);
				}
			}

			Map<String, List<XMLElement>> xmlElementListMap = new LinkedHashMap<String, List<XMLElement>>();
			List<XMLElement> elementList = parseXML(xmlElement.getElements());//xmlElement의 내부 엘리먼트를 파싱
			List<XMLElement> list;

			//파싱되어 반환된 XMLElement 객체들 중 태그 이름이 같은 객체끼리 List로 묶는다
			for (XMLElement element : elementList) {
				if (xmlElementListMap.containsKey(element.getName())) {
					list = xmlElementListMap.get(element.getName());
					list.add(element);
				} else {
					list = new ArrayList<XMLElement>();
					list.add(element);
				}
				xmlElementListMap.put(element.getName(), list);
			}

			Iterator<String> iter = xmlElementListMap.keySet().iterator();

			while (iter.hasNext()) {
				List<Object> objList;
				String key = iter.next();
				list = xmlElementListMap.get(key);

				//List 내의 XMLElement 객체의 개수가 1보다 클 경우
				if (list.size() > 1) {
					//각 XMLElement를  Map에 포함될 Object로 구성하여 Object List에 추가
					objList = new ArrayList<Object>();
					for (XMLElement element : list) {
						objList.add(makeElementsObj(element));
					}
					//Object List를 Map에 추가
					objMap.put(list.get(0).getName(), objList);
				} else {//List 내의 XMLElement 객체의 개수가 1개일 경우
					//XMLElement List에서 XMLElement 객체를 추출하여 Map에 추가
					objMap.put(list.get(0).getName(), makeElementsObj(list.get(0)));
				}
			}
		}

		return objMap;
	}

	//XML 문자열이 well-formed 한지 검사하는 유효성 검사기
	static public Boolean isWellFormedXML(String xml) {
		try {
			// Create a new factory to create parsers
			DocumentBuilderFactory dBF = DocumentBuilderFactory.newInstance();
			// Use the factory to create a parser (builder) and use
			// it to parse the document.
			DocumentBuilder builder = dBF.newDocumentBuilder();
			// builder.setErrorHandler(new MyErrorHandler());
			InputSource is = new InputSource(new StringReader(xml));
			Document doc = builder.parse(is);
			System.out.println("xml is well-formed!");
			return true;
		} catch (Exception e) {
			System.out.println("xml isn't well-formed!");
			// System.exit(1);
			System.out.println(e.getMessage());
			// e.printStackTrace();
			return false;
		}
	}
	
	//XML내에서 특정 문자와 상호 교환 가능한 문자열을 특정 문자로 치환하는 함수 ex) &lt; <=> '<', &gt <=> '>' .... 
	static public String changeToJSONString(String str) {
		str = str.replace("&lt;", "<").replace("&gt;", ">").replaceAll("&amp;", "&")
				.replaceAll("&apos;", "'").replaceAll("&quot;", "\"");
		
		return str;
	}
	
	//태그 내에 네임스페이스가 사용되었는지 확인하는 함수
	static public String checkNameSpace(String str) {
		if(str.indexOf(":") != -1) {
			throw new InvalidNameSpaceOfXMLException("네임스페이스를 지원하지 않습니다.");
		}
		return str;
	}
}
