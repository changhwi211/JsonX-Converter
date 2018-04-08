package com.example.demo.cache;

public class Status {
	private String name;
	private int successCount = 0;
	private int failureCount = 0;
	
	public Status(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getSuccessCount() {
		return successCount;
	}
	public void setSuccessCount(int successCount) {
		this.successCount = successCount;
	}
	public int getFailureCount() {
		return failureCount;
	}
	public void setFailureCount(int failureCount) {
		this.failureCount = failureCount;
	}
	public void addSuccessCount() {
		this.successCount += 1;
	}
	public void addFailureCount() {
		this.failureCount += 1;
	}
	
}
