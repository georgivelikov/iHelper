package com.ihelper;

import org.springframework.boot.SpringApplication;

public class TestIhelperApplication {

	public static void main(String[] args) {
		SpringApplication.from(IhelperApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
