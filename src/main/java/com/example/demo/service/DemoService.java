package com.example.demo.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
public class DemoService {

	
	public int i=3;
	
	
	public String invokeCallWithCB() {
		i++;
		System.out.println(i);
		
		if(i%2 ==0 && i<20 )
			throw new HttpClientErrorException(HttpStatus.BAD_GATEWAY);
		
	
		return "Call->Service";
	}

}
