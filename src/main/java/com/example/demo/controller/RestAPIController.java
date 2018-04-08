package com.example.demo.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.cache.Status;
import com.example.demo.cache.StatusRepository;
import com.example.demo.err.InvalidKeyOfJSONException;
import com.example.demo.err.InvalidNameSpaceOfXMLException;
import com.example.demo.json.JSONUtils;
import com.example.demo.xml.XMLUtils;

@RestController
public class RestAPIController {
	private static final Logger logger = LoggerFactory.getLogger(RestAPIController.class);

	@Autowired
	StatusRepository statusReopository;

	@RequestMapping(value = "/xml2json", method = RequestMethod.POST, consumes = "application/xml", produces = "application/json")
	public ResponseEntity<Map<String, Object>> xtoj(HttpServletRequest request, @RequestBody String xml) {
		long start = System.currentTimeMillis();

		Map<String, Object> map;

		if (XMLUtils.isWellFormedXML(xml)) {
			try {
				map = XMLUtils.convertXMLToJSON(xml);
				long end = System.currentTimeMillis();

				System.out.println("실행 시간 : " + (end - start) / 1000.0);
				return new ResponseEntity<Map<String, Object>>(map, HttpStatus.OK);
			} catch (InvalidNameSpaceOfXMLException e) {
				map = new HashMap<String, Object>();
				map.put("error", e.getMessage());
				System.out.println(e.getMessage());
				return new ResponseEntity<Map<String, Object>>(map, HttpStatus.BAD_REQUEST);
			} catch (Exception e) {
				map = new HashMap<String, Object>();
				map.put("error", e.getMessage());
				System.out.println(e.getMessage());
				return new ResponseEntity<Map<String, Object>>(map, HttpStatus.BAD_REQUEST);
			}
		} else {
			map = new HashMap<String, Object>();
			map.put("error", "well-formed한 XML이 아닙니다.");
			return new ResponseEntity<Map<String, Object>>(map, HttpStatus.BAD_REQUEST);
		}

	}

	@RequestMapping(value = "/json2xml", method = RequestMethod.POST, consumes = "application/json", produces = "application/xml")
	public ResponseEntity<String> jtox(HttpServletRequest request, @RequestBody String json) {
		long start = System.currentTimeMillis();
		String xml;

		if (JSONUtils.isJSONValid(json)) {
			try {
				System.out.println("시작");
				xml = "<?xml version='1.0' encoding='utf-8'?>\r\n" + JSONUtils.convertJSONToXML(json);

				long end = System.currentTimeMillis();
				System.out.println("실행 시간 : " + (end - start) / 1000.0);
				System.out.println(xml);
				return new ResponseEntity<String>(xml, HttpStatus.OK);
			} catch (InvalidKeyOfJSONException e) {
				System.out.println(e.getMessage());
				xml = "<error>" + e.getMessage() + "</error>";
				return new ResponseEntity<String>(xml, HttpStatus.BAD_REQUEST);
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				xml = "<error>변환오류입니다</error>";
				return new ResponseEntity<String>(xml, HttpStatus.BAD_REQUEST);
			}

		} else {
			xml = "<error>유효하지 않은 JSON입니다.</error>";
			return new ResponseEntity<String>(xml, HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(value = "/status", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<Status> status(@RequestParam("name") String name, @RequestParam("result") String result) {
		logger.info(".... Fetching");
		Status status = statusReopository.getByName(name);
		logger.info(".... Complete Fetching");

		if(result.equals("load")) {
			
		}else if (result.equals("success")) {
			status.addSuccessCount();
		}else {
			status.addFailureCount();
		}
		return new ResponseEntity<Status>(status, HttpStatus.OK);
	}

	@RequestMapping(value = "/loadXMLFile", method = RequestMethod.POST, produces = "application/xml")
	public ResponseEntity<String> loadXMLFile(HttpServletRequest request, @RequestParam("file") MultipartFile file)
			throws IOException {
		String content = new String(file.getBytes(),"utf-8");
		
		System.out.println("loadXMLFile success \n" + content);
		return new ResponseEntity<String>(content, HttpStatus.OK);

	}

	@RequestMapping(value = "/loadJSONFile", method = RequestMethod.POST, produces = "application/json")
	public ResponseEntity<String> uploadJSONFile(HttpServletRequest request, @RequestParam("file") MultipartFile file)
			throws IOException {
		String content = new String(file.getBytes(),"utf-8");

		System.out.println("loadJsonFile success \n" + content);
		return new ResponseEntity<String>(content, HttpStatus.OK);
	}

}
