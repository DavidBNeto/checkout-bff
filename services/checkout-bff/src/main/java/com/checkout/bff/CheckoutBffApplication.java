package com.checkout.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CheckoutBffApplication {

	public static void main(String[] args) {
		SpringApplication.run(CheckoutBffApplication.class, args);
	}

}
