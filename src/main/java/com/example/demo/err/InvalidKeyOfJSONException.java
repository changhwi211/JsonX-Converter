package com.example.demo.err;

public class InvalidKeyOfJSONException extends RuntimeException {
	public InvalidKeyOfJSONException() {}
	public InvalidKeyOfJSONException(String message) {
		super(message);
	}
}
