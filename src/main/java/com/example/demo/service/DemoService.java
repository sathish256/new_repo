package com.example.demo.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
public class DemoService {

	
	public int i=-1;
	
	
	public String invokeCallWithCB() {
		i++;
		
		if(i>3 && i<9 )
			throw new HttpClientErrorException(HttpStatus.BAD_GATEWAY);
		
	
		return "Call->Service";
	}

}
