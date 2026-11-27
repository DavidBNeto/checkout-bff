package com.checkout.bff;

import org.springframework.boot.SpringApplication;

public class TestCheckoutBffApplication {

	public static void main(String[] args) {
		SpringApplication.from(CheckoutBffApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
